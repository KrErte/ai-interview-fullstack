"""Download the pinned raw inputs for build.py into tools/onet-import/.cache/ (skips files already there)."""
import shutil
import urllib.request

from build import CACHE

ONET_VERSION = '29_3'
AEI_REVISION = '4e5f69fab13a9c3f7d0dcc31a6f893a9818738f6'
AEI_BASE = f'https://huggingface.co/datasets/Anthropic/EconomicIndex/resolve/{AEI_REVISION}/'
GPTS_REVISION = '0471612fef3cc22b74fb884d27bff9dbd3770582'
GPTS_BASE = f'https://raw.githubusercontent.com/openai/GPTs-are-GPTs/{GPTS_REVISION}/'

SOURCES = {
    f'db_{ONET_VERSION}_text.zip': f'https://www.onetcenter.org/dl_files/database/db_{ONET_VERSION}_text.zip',
    'robot_exposure_tasks.csv': AEI_BASE + 'robot_exposure/robot_exposure_tasks.csv',
    'task_penetration.csv': AEI_BASE + 'labor_market_impacts/task_penetration.csv',
    'full_labelset.tsv': GPTS_BASE + 'data/full_labelset.tsv',
}


def fetch():
    CACHE.mkdir(parents=True, exist_ok=True)
    for name, url in SOURCES.items():
        target = CACHE / name
        if target.exists():
            print(f'cached   {name}')
            continue
        partial = target.with_suffix(target.suffix + '.part')
        with urllib.request.urlopen(url) as response, partial.open('wb') as f:
            shutil.copyfileobj(response, f)
        partial.rename(target)
        print(f'fetched  {name} ({target.stat().st_size:,} bytes)')


if __name__ == '__main__':
    fetch()
