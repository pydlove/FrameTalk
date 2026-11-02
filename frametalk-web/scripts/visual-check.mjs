import { chromium } from 'playwright'

const baseUrl = process.env.FRAMETALK_WEB_URL ?? 'http://127.0.0.1:32588'
const email = process.env.FRAMETALK_TEST_EMAIL ?? 'creator@frametalk.local'
const password = process.env.FRAMETALK_TEST_PASSWORD
const outputDir = process.env.FRAMETALK_SCREENSHOT_DIR ?? '/tmp'

if (!password) {
  throw new Error('Set FRAMETALK_TEST_PASSWORD before running visual:check')
}

const browser = await chromium.launch({
  channel: 'chrome',
  headless: true,
})

const context = await browser.newContext({
  viewport: { width: 1280, height: 800 },
  deviceScaleFactor: 1,
})
const page = await context.newPage()
const consoleErrors = []
page.on('console', (message) => {
  if (
    message.type() === 'error'
    && !message.text().includes('401 (Unauthorized)')
  ) {
    consoleErrors.push(message.text())
  }
})
page.on('pageerror', (error) => consoleErrors.push(error.message))

async function assertNoHorizontalOverflow(label) {
  const dimensions = await page.evaluate(() => ({
    viewport: window.innerWidth,
    document: document.documentElement.scrollWidth,
  }))
  if (dimensions.document > dimensions.viewport) {
    throw new Error(
      `${label} has horizontal overflow: ${dimensions.document} > ${dimensions.viewport}`,
    )
  }
  return dimensions
}

await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
await page.screenshot({
  path: `${outputDir}/frametalk-login-desktop.png`,
  fullPage: true,
})
const desktop = await assertNoHorizontalOverflow('login desktop')

await page.setViewportSize({ width: 390, height: 844 })
await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
await page.screenshot({
  path: `${outputDir}/frametalk-login-mobile.png`,
  fullPage: true,
})
const mobile = await assertNoHorizontalOverflow('login mobile')

await page.emulateMedia({ colorScheme: 'dark' })
await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
await page.screenshot({
  path: `${outputDir}/frametalk-login-dark.png`,
  fullPage: true,
})
const dark = await assertNoHorizontalOverflow('login dark')

await page.emulateMedia({ colorScheme: 'light' })
await page.setViewportSize({ width: 1280, height: 800 })
await page.goto(`${baseUrl}/register`, { waitUntil: 'networkidle' })
await page.screenshot({
  path: `${outputDir}/frametalk-register-desktop.png`,
  fullPage: true,
})
const register = await assertNoHorizontalOverflow('register desktop')

await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
await page.getByLabel('邮箱').fill(email)
await page.getByLabel('密码', { exact: true }).fill(password)
await page.getByRole('button', { name: '进入工作台' }).click()
await page.waitForURL('**/projects')
await page.screenshot({
  path: `${outputDir}/frametalk-projects-desktop.png`,
  fullPage: true,
})
const projects = await assertNoHorizontalOverflow('projects desktop')

await page.goto(`${baseUrl}/settings/security`, {
  waitUntil: 'networkidle',
})
await page.screenshot({
  path: `${outputDir}/frametalk-settings-security.png`,
  fullPage: true,
})
const settings = await assertNoHorizontalOverflow('settings security')

await page.goto(`${baseUrl}/settings/appearance`, {
  waitUntil: 'networkidle',
})
await page.getByRole('button', { name: '深色' }).click()
await page.waitForFunction(() => document.documentElement.dataset.theme === 'dark')
await page.screenshot({
  path: `${outputDir}/frametalk-settings-appearance-dark.png`,
  fullPage: true,
})
const appearanceDark = await assertNoHorizontalOverflow(
  'settings appearance dark',
)

await page.setViewportSize({ width: 390, height: 844 })
await page.reload({ waitUntil: 'networkidle' })
await page.screenshot({
  path: `${outputDir}/frametalk-settings-mobile.png`,
  fullPage: true,
})
const settingsMobile = await assertNoHorizontalOverflow('settings mobile')

if (consoleErrors.length > 0) {
  throw new Error(`Browser console errors:\n${consoleErrors.join('\n')}`)
}

console.log(JSON.stringify({
  desktop,
  mobile,
  dark,
  register,
  projects,
  settings,
  appearanceDark,
  settingsMobile,
  screenshots: [
    `${outputDir}/frametalk-login-desktop.png`,
    `${outputDir}/frametalk-login-mobile.png`,
    `${outputDir}/frametalk-login-dark.png`,
    `${outputDir}/frametalk-register-desktop.png`,
    `${outputDir}/frametalk-projects-desktop.png`,
    `${outputDir}/frametalk-settings-security.png`,
    `${outputDir}/frametalk-settings-appearance-dark.png`,
    `${outputDir}/frametalk-settings-mobile.png`,
  ],
}, null, 2))

await browser.close()
