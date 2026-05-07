import json
import sys
import time
import requests

def fetch_sge_price(max_retries=5, delay=1.0):
    url = "https://www.sge.com.cn/graph/quotations"
    payload = {"instid": "Au99.99"}
    headers = {
        "Accept": "application/json, text/javascript, */*; q=0.01",
        "Accept-Encoding": "gzip, deflate, br",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Cache-Control": "no-cache",
        "Connection": "keep-alive",
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "Origin": "https://www.sge.com.cn",
        "Pragma": "no-cache",
        "Referer": "https://www.sge.com.cn/",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "X-Requested-With": "XMLHttpRequest",
    }

    last_error = None
    for attempt in range(max_retries):
        try:
            r = requests.get(url, params=payload, headers=headers, timeout=15)
            if r.status_code != 200:
                last_error = f"HTTP {r.status_code}"
                time.sleep(delay)
                continue
            data = r.json()
            if not data.get("times") or not data.get("data"):
                last_error = "empty response"
                time.sleep(delay)
                continue
            times = data["times"]
            prices = data["data"]
            update_time = data.get("delaystr", "")
            return {
                "source": "上海黄金交易所 Au99.99",
                "price": float(prices[-1]),
                "time": update_time,
            }
        except Exception as e:
            last_error = str(e)
            time.sleep(delay)
    return {"error": f"all {max_retries} retries failed: {last_error}"}

result = fetch_sge_price()
print(json.dumps(result, ensure_ascii=False))
if "error" in result:
    sys.exit(1)
