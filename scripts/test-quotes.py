import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('adapter', Path(__file__).with_name('export-quotes.py'))
adapter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(adapter)


class QuotesTest(unittest.TestCase):
    def test_trade_not_trial(self):
        value = adapter.normalize('0050', {'symbol': '0050', 'lastPrice': 999,
            'lastTrade': {'price': 60.25, 'time': 1758519000000000}})
        self.assertEqual(value['price'], 60.25)
        self.assertEqual(value['asOf'], '2025-09-22T05:30:00+00:00')

    def test_invalid_response(self):
        for data in ({}, {'symbol': '0050', 'closePrice': float('nan')},
                     {'symbol': '0050', 'closePrice': 60}):
            with self.assertRaises(ValueError):
                adapter.normalize('0050', data)


if __name__ == '__main__':
    unittest.main()
