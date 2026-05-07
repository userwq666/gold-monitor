import json
import sys

try:
    import akshare as ak
    df = ak.spot_quotations_sge('Au99.99')
    latest = df.iloc[-1]
    result = {
        "source": "上海黄金交易所 Au99.99",
        "price": float(latest["现价"]),
        "time": str(latest["更新时间"])
    }
    print(json.dumps(result, ensure_ascii=False))
except Exception as e:
    print(json.dumps({"error": str(e)}, ensure_ascii=False))
    sys.exit(1)
