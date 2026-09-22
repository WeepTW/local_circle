"""Read-only E.SUN SDK adapter. Credentials and live output stay outside public assets."""
import argparse
from configparser import ConfigParser
from datetime import datetime, timezone
import json
import math
from pathlib import Path


def normalize(symbol, data):
    # Prefer actual trades; lastPrice may contain a trial-auction price.
    trade = data.get('lastTrade') or {}
    price = trade.get('price', data.get('closePrice'))
    stamp = trade.get('time', data.get('closeTime'))
    if data.get('symbol') != symbol or isinstance(price, bool) or not isinstance(price, (float, int)) or not math.isfinite(price) or not 0 < price <= 999999999999:
        raise ValueError('Invalid quote')
    if isinstance(stamp, bool) or not isinstance(stamp, (float, int)) or not math.isfinite(stamp):
        raise ValueError('Missing trade timestamp')
    as_of = datetime.fromtimestamp(stamp / 1_000_000, timezone.utc)
    if as_of.timestamp() > datetime.now(timezone.utc).timestamp() + 60:
        raise ValueError('Future trade timestamp')
    return {'symbol': symbol, 'price': price, 'asOf': as_of.isoformat()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True, type=Path)
    args = parser.parse_args()
    # Fixed private destination prevents accidentally exporting account config to Pages.
    destination = Path(__file__).resolve().parent.parent / 'tmp' / 'quotes.live.json'
    try:
        from esun_marketdata import EsunMarketdata
        config = ConfigParser()
        if not config.read(args.config):
            raise ValueError('Missing configuration')
        sdk = EsunMarketdata(config)
        sdk.login()
        quotes = [normalize(symbol, sdk.rest_client.stock.intraday.quote(symbol=symbol))
                  for symbol in ('0050', '0052')]
        destination.parent.mkdir(exist_ok=True)
        temporary = destination.with_suffix('.pending')
        temporary.write_text(json.dumps({'source': 'esun', 'quotes': quotes}, ensure_ascii=False), encoding='utf-8')
        temporary.replace(destination)
        print('Quotes exported to tmp/quotes.live.json; no public upload performed.')
    except Exception:
        # Provider errors may contain credentials; deliberately do not echo them.
        raise SystemExit('Quote export failed. Check SDK installation, local credentials and market-data permission.') from None


if __name__ == '__main__':
    main()
