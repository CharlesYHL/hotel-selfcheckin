import HmacSHA256 from 'crypto-js/hmac-sha256'

const SIGN_SECRET = 'hotel-sign-secret-change-in-production'
const APP_ID = 'hotel-h5'

/**
 * 生成 HMAC-SHA256 请求签名
 * 签名算法: HMAC-SHA256(signSecret, appId + path + timestamp)
 */
export function generateSign(path: string, timestamp: number): string {
  const raw = APP_ID + path + timestamp
  return HmacSHA256(raw, SIGN_SECRET).toString()
}

/**
 * 为请求头添加签名参数
 */
export function getSignHeaders(path: string): Record<string, string> {
  const timestamp = Date.now()
  return {
    'X-Sign': generateSign(path, timestamp),
    'X-Timestamp': String(timestamp),
    'X-App-Id': APP_ID
  }
}