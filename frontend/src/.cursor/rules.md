# Deployment Workflow (Windows + SSH)

When I ask you to implement a feature or fix and deploy it, ALWAYS follow these steps:

1. Make code changes in the current project folder.

2. Run backend tests:
   cd backend
   ./gradlew test

3. Run frontend tests/lint:
   cd ../frontend
   npm run lint

4. If all tests pass:
   cd ..
   git add .
   git commit -m "<short message>"
   git push origin main

5. Then deploy to production by running:
   pwsh -Command "./deploy-remote.ps1"

The deploy-remote.ps1 script connects to the VPS and runs /root/deploy.sh which deploys frontend + backend.

ALWAYS confirm completion and show the deploy output at the end.
