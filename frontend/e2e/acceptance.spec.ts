import {test,expect} from '@playwright/test'
test('save, edit, cancel/confirm delete, identity and mobile navigation',async({page,request})=>{
 const h={'X-Demo-User-Id':'1'}
 const existing=await (await request.get('/api/v1/preferences',{headers:h})).json()
 for(const p of existing)await request.delete(`/api/v1/preferences/${p.preferenceId}?version=${p.version}`,{headers:h})
 await page.goto('/preferences');await expect(page.getByText('從第一個喜好開始')).toBeVisible()
 await page.getByRole('button',{name:'＋ 保存喜好'}).click()
 await page.getByLabel('預計數量',{exact:true}).fill('5')
 await page.getByRole('button',{name:'確認保存',exact:true}).click()
 await expect(page.locator('tbody tr')).toHaveCount(1);await expect(page.locator('tbody')).toContainText('300.3');await expect(page.locator('tbody')).toContainText('user@example.test');await expect(page.locator('tbody')).toContainText('元大台灣50 ETF')
 await page.screenshot({path:'../docs/screenshots/preferences.png',fullPage:true})
 await page.getByRole('button',{name:'編輯',exact:true}).click();await page.getByRole('combobox',{name:'商品',exact:true}).selectOption('2');await page.getByRole('combobox',{name:'預計扣款帳號',exact:true}).selectOption('11');await page.getByLabel('預計數量',{exact:true}).fill('3');await page.getByRole('button',{name:'確認保存',exact:true}).click()
 await expect(page.locator('tbody')).toContainText('540.54');await expect(page.locator('tbody')).toContainText('******1122')
 await page.getByLabel('檢視角色').selectOption('2');await expect(page.getByText('從第一個喜好開始')).toBeVisible()
 await page.getByLabel('檢視角色').selectOption('1');await expect(page.locator('tbody tr')).toHaveCount(1)
 await page.getByRole('button',{name:'刪除',exact:true}).click();await page.getByRole('button',{name:'取消',exact:true}).click();await expect(page.locator('tbody tr')).toHaveCount(1)
 await page.getByRole('button',{name:'刪除',exact:true}).click();await page.getByRole('button',{name:'確認刪除',exact:true}).click();await expect(page.getByText('從第一個喜好開始')).toBeVisible()
 await page.setViewportSize({width:390,height:844});await page.getByRole('link',{name:'帳戶參照',exact:true}).click();await expect(page.getByText('******9666',{exact:true})).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true)
 await page.screenshot({path:'../docs/screenshots/mobile-accounts.png',fullPage:true})
})
test('admin ownership and stored XSS render safely in the actual browser',async({page,request})=>{
 const admin={'X-Demo-User-Id':'3'},user={'X-Demo-User-Id':'1'}
 const original=(await (await request.get('/api/v1/products',{headers:admin})).json()).find((p:{productId:number})=>p.productId===1)
 const attack='<img src=x onerror="window.__xss=1">'
 const errors:string[]=[];page.on('pageerror',e=>errors.push(e.message))
 try {
  await page.goto('/preferences');await page.getByLabel('檢視角色').selectOption('3');await page.getByRole('link',{name:'商品管理',exact:true}).click()
  await expect(page.getByRole('button',{name:'編輯商品',exact:true})).toHaveCount(2)
  await page.getByRole('button',{name:'編輯商品',exact:true}).first().click();await page.getByLabel('產品名稱',{exact:true}).fill(attack);await page.getByRole('button',{name:'確認更新',exact:true}).click();await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('heading',{name:attack,exact:true})).toBeVisible();expect(await page.evaluate(()=>Object.hasOwn(window,'__xss'))).toBe(false);await expect(page.locator('article img')).toHaveCount(0)
  const product=(await (await request.get('/api/v1/products',{headers:admin})).json()).find((p:{productId:number})=>p.productId===1)
  for(const uid of ['1','4'])expect((await request.patch('/api/v1/admin/products/1',{headers:{'X-Demo-User-Id':uid},data:{productName:'forbidden',price:60,feeRate:.001,active:true,version:product.version}})).status()).toBe(403)
  await page.getByLabel('檢視角色').selectOption('4');await page.getByRole('link',{name:'商品管理',exact:true}).click();await expect(page.getByRole('button',{name:'編輯商品',exact:true})).toHaveCount(1)
  await page.screenshot({path:'../docs/screenshots/admin.png',fullPage:true});expect(errors).toEqual([])
  const response=await request.get('/preferences');expect(response.headers()['content-security-policy']).toContain("script-src 'self'")
  expect((await request.post('/api/v1/preferences',{headers:user,data:{productId:1,accountId:20,plannedQuantity:5}})).status()).toBe(404)
 } finally {
  const now=(await (await request.get('/api/v1/admin/products',{headers:admin})).json()).find((p:{productId:number})=>p.productId===1)
  await request.patch('/api/v1/admin/products/1',{headers:admin,data:{productName:original.productName,price:original.price,feeRate:original.feeRate,active:original.active,version:now.version}})
 }
})
test('network error is visible and retry recovers',async({page})=>{
 await page.route('**/api/v1/preferences',route=>route.abort())
 await page.goto('/preferences');await expect(page.getByRole('alert')).toContainText('無法連線')
 await page.unroute('**/api/v1/preferences');await page.getByRole('button',{name:'重新整理'}).click();await expect(page.getByRole('heading',{name:'保存的規劃',exact:true})).toBeVisible();await expect(page.getByRole('alert')).toHaveCount(0)
})
