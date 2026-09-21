"""Bounded read-only concurrency check against a running local instance."""
import concurrent.futures,json,os,time,urllib.request
url=os.environ.get('BASE_URL','http://127.0.0.1:8088')+'/api/v1/products'
def check(_):
 start=time.monotonic()
 with urllib.request.urlopen(urllib.request.Request(url,headers={'X-Demo-User-Id':'1'}),timeout=15) as response:
  rows=json.load(response)
  assert response.status==200 and len(rows)==3
 return time.monotonic()-start
with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool: timings=sorted(pool.map(check,range(100)))
print(json.dumps({'requests':100,'concurrency':10,'failures':0,'p95_ms':round(timings[94]*1000),'max_ms':round(max(timings)*1000)}))
