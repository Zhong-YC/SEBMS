"""HTTP + real database integration. Creates named acceptance fixtures; keeps history."""
import json,urllib.request,urllib.error,http.cookiejar,time,concurrent.futures,pathlib
BASE='http://127.0.0.1:8080/api'
class Client:
 def __init__(self):self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
 def call(self,method,path,data=None,expected=200):
  request=urllib.request.Request(BASE+path,data=None if data is None else json.dumps(data).encode(),method=method,headers={'Content-Type':'application/json','Origin':'http://127.0.0.1:5173'})
  try:
   with self.opener.open(request,timeout=15) as r:status=r.status;body=json.load(r)
  except urllib.error.HTTPError as e:status=e.code;body=json.load(e)
  if expected is not None:assert status==expected,(method,path,status,body)
  return status,body
 def ok(self,method,path,data=None):return self.call(method,path,data)[1]['data']
 def login(self,name,password):return self.ok('POST','/auth/login',dict(username=name,password=password))
results=[]
def passed(name):results.append(name);print('PASS',name)
anon=Client();anon.call('GET','/equipment',expected=401);passed('未登录拒绝访问')
admin=Client();admin.login('admin','Admin123!')
student=Client();student.login('student','Student123!')
student.call('GET','/users',expected=403);passed('普通用户禁止访问管理接口')
own=student.ok('GET','/auth/me')
updated=student.ok('PUT','/auth/profile',{'displayName':own['displayName']})
assert updated['id']==own['id'] and updated['role']=='USER';passed('本人资料修改不改变角色')
status,_=student.call('PUT','/auth/profile',{'displayName':own['displayName'],'role':'ADMIN'},None);assert status>=400;passed('拒绝通过资料接口提升权限')
cross=urllib.request.Request(BASE+'/auth/logout',data=b'{}',method='POST',headers={'Content-Type':'application/json','Origin':'https://untrusted.example'})
try:student.opener.open(cross,timeout=10);raise AssertionError('跨站请求未拒绝')
except urllib.error.HTTPError as e:assert e.code==403
passed('跨站状态修改请求拒绝')
suffix=str(int(time.time()))
category=admin.ok('POST','/categories',{'name':'验收_'+suffix})
equipment=admin.ok('POST','/equipment',{'name':'验收最后一件_'+suffix,'description':'自动验收数据，可停用保留历史','categoryId':category['id'],'totalQuantity':1,'enabled':True})
othername='qa_'+suffix
other=admin.ok('POST','/users',{'username':othername,'password':'Test12345!','displayName':'验收用户','role':'USER','enabled':True})
second=Client();second.login(othername,'Test12345!')
with concurrent.futures.ThreadPoolExecutor(2) as pool:
 futures=[pool.submit(c.call,'POST','/borrows',{'equipmentId':equipment['id']},None) for c in [student,second]]
 responses=[f.result() for f in futures]
assert sum(status==200 for status,_ in responses)==1,responses
winner=[student,second][next(i for i,r in enumerate(responses) if r[0]==200)]
loser=second if winner is student else student
record=next(body['data'] for status,body in responses if status==200)
passed('并发借最后1件仅1人成功')
status,_=admin.call('PUT',f"/equipment/{equipment['id']}",dict(name=equipment['name'],description=equipment['description'],categoryId=category['id'],totalQuantity=0,enabled=True),None)
assert status>=400;passed('总量不能低于已借出量')
status,_=loser.call('POST','/borrows',{'equipmentId':equipment['id']},None);assert status>=400;passed('库存不足拒绝借用')
status,_=loser.call('POST',f"/borrows/{record['id']}/return",{},None);assert status>=400;passed('禁止归还他人记录')
allmine=loser.ok('GET','/borrows');assert all(r['id']!=record['id'] for r in allmine);passed('借用记录归属隔离')
winner.ok('POST',f"/borrows/{record['id']}/return",{})
status,_=winner.call('POST',f"/borrows/{record['id']}/return",{},None);assert status>=400;passed('重复归还拒绝')
items=admin.ok('GET','/equipment');current=next(e for e in items if e['id']==equipment['id']);assert current['availableQuantity']==current['totalQuantity']==1;passed('借还后库存恢复且一致')
status,_=admin.call('DELETE',f"/equipment/{equipment['id']}",expected=None);assert status>=400;passed('有历史器材禁止硬删除')
status,_=admin.call('DELETE',f"/categories/{category['id']}",expected=None);assert status>=400;passed('有关联器材分类禁止删除')
admin.ok('PUT',f"/equipment/{equipment['id']}",dict(name=equipment['name'],description=equipment['description'],categoryId=category['id'],totalQuantity=1,enabled=False))
status,_=student.call('POST','/borrows',{'equipmentId':equipment['id']},None);assert status>=400;passed('停用器材禁止借出')
stats=admin.ok('GET','/stats');assert stats['totalQuantity']==stats['availableQuantity']+stats['borrowedQuantity'];passed('统计库存关系一致')
student.ok('POST','/auth/logout',{});student.call('GET','/auth/me',expected=401);passed('退出使会话失效')
out=pathlib.Path(__file__).resolve().parents[1]/'.local'/'smoke-result.json'
out.write_text(json.dumps({'passed':results,'count':len(results),'fixture':suffix},ensure_ascii=False,indent=2),encoding='utf8')
