# Upgrade notes

## FP-28: DB_PASSWORD and JWT_SECRET are now required (Windows / PowerShell)

After pulling this change, `docker compose up` refuses to start until `.env` (repo root, next to
`docker-compose.yml`) sets **both** `DB_PASSWORD` and `JWT_SECRET`:

```
required variable DB_PASSWORD is missing a value: DB_PASSWORD must be set in .env (see .env.example)
```

A missing `JWT_SECRET` fails the same way (`required variable JWT_SECRET is missing a value`).
The check applies to `docker compose up` and `docker compose down`, so create `.env` first.

**Order for an existing install:** do steps 1, 3 and 4 with `DB_PASSWORD=ai123` first; change the
database password afterwards (optional part of step 2) if you want to.

### 1. Create .env

```powershell
Copy-Item .env.example .env
notepad .env
```

Edit it in Notepad (or VS Code). If you write `.env` from PowerShell, use
`Set-Content -Encoding ascii` (or `utf8` in PowerShell 7); the `>` redirect in Windows PowerShell 5.1
writes UTF-16, which docker compose cannot read.

### 2. DB_PASSWORD: keep your existing database

Postgres sets the password only when its data volume is first created. If you ran this stack
before, your volume was created with the old built-in password `ai123`.

**Fastest path (keeps all data):** put this in `.env`:

```
DB_PASSWORD=ai123
```

**Optional: change it to a real password, keeping the data.** Start the stack with
`DB_PASSWORD=ai123` first (step 4), then change the password inside the database (psql inside the
container connects over the local socket and needs no password):

```powershell
docker compose exec postgres psql -U ai -d ai -c "ALTER USER ai PASSWORD 'MyNewStrongPassword';"
```

In PowerShell, the double quotes wrap the whole SQL and the single quotes are the SQL string. Use a
password without `'`, `"`, `$` or backticks so no extra escaping is needed. To avoid quoting entirely
(and keep the password out of your shell history), use psql's interactive prompt:

```powershell
docker compose exec postgres psql -U ai -d ai
# then in psql:   \password ai      (type the new password twice), then  \q
```

Then set `DB_PASSWORD=MyNewStrongPassword` in `.env` and run `docker compose up -d` again so the
backend uses it.

**Start fresh instead (DELETES ALL DATA):** `docker compose down -v`, then choose any strong
`DB_PASSWORD` before the next `up`.

### 3. JWT_SECRET

At least 32 bytes. In PowerShell 7.2+ (.NET 6 or newer):

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

In Windows PowerShell 5.1 or PowerShell 7.0/7.1 (where that static `GetBytes(48)` does not exist):

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Put the output in `.env` with the `base64:` prefix, so the backend uses the 48 decoded random bytes:

```
JWT_SECRET=base64:<output>
```

The prefix must be lowercase `base64:`. `BASE64:` is not recognised, and the whole text is then
used as a raw key.

Alternative (Git Bash / WSL / any machine with OpenSSL): `openssl rand -base64 48`, and use the output
the same way (`JWT_SECRET=base64:<output>`; without the prefix the 64-character text itself is the key,
which is also accepted).

The backend refuses to start if the key is shorter than 32 bytes, contains `change-me`, or is the old
built-in key (also when given base64-encoded).

### 4. Start, from the same folder as before

```powershell
docker compose up -d --build
```

Run this from the **same folder** as before (or pass the same `-p <project>`): compose names the
database volume `<project>_pgdata-local`, where `<project>` defaults to the folder name. A different
folder or project name creates a new, empty database instead of reusing yours. Check with
`docker volume ls` that the volume you expect exists.

### 5. Everyone logs in again

The signing key changed, so all existing login tokens are invalid: users (and the browser) get 401
and have to log in again. Accounts and data are not affected.
