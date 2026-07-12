// API 响应格式
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  success: boolean
  degraded: boolean
}

// 登录
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: string
  name: string
  role: string
  phone: string
}

// 用户信息
export interface UserInfo {
  userId: string
  name: string
  role: string
  phone: string
  accessToken: string
  refreshToken: string
}

// 房型
export interface RoomType {
  roomTypeId: string
  hotelId: string
  roomTypeCode: string
  roomTypeName: string
  baseCapacity: number
  maxCapacity: number
  bedType: string
  availableCount: number
  maxRooms: number
}

// 房间
export interface RoomDTO {
  roomId: string
  roomNo: string
  hotelId: string
  roomTypeId: string
  floorNo: string
  roomStatus: number
  roomStatusDesc: string
  direction: string
  maxGuest: number
  isSmokeFree: number
  createdTime: string
}

// 订单
export interface OrderCreateRequest {
  hotelId: string
  memberId?: string
  roomTypeId: string
  checkInDate: string
  checkOutDate: string
  roomCount?: number
  adults?: number
  children?: number
  contactName?: string
  contactPhone?: string
  specialRequest?: string
  orderAmount?: number
  sourceType?: number
  sourceChannel?: string
  orderNo?: string
}

export interface OrderResponse {
  orderId: string
  orderNo: string
  hotelId: string
  memberId: string
  roomTypeId: string
  roomTypeName: string
  checkInDate: string
  checkOutDate: string
  nights: number
  roomCount: number
  orderAmount: number
  paidAmount: number
  orderStatus: number
  orderStatusDesc: string
  createdTime: string
}

// 支付
export interface PaymentResponse {
  paymentId: string
  paymentNo: string
  orderId: string
  amount: number
  status: number
  statusDesc: string
  tradeNo: string
  payTime: string
  createdTime: string
}

// 入住
export interface CheckInRequest {
  orderId: string
  hotelId: string
  roomId: string
  memberId?: string
  guests: GuestInfo[]
  checkinChannel?: number
  strategy?: string
}

export interface GuestInfo {
  guestName: string
  guestType: number
  idCardType: number
  idCardNo: string
  gender?: number
  phone?: string
}

export interface CheckInResponse {
  checkinId: string
  checkinNo: string
  orderId: string
  roomId: string
  roomNo: string
  hotelId: string
  memberId: string
  checkInTime: string
  checkOutTime: string
  status: number
  statusDesc: string
  verifyStatus: number
  verifyStatusDesc: string
  cardNo: string
  createdTime: string
}

// 房卡
export interface CardResponse {
  cardId: string
  cardNo: string
  hotelId: string
  checkinId: string
  roomId: string
  roomNo: string
  validFrom: string
  validTo: string
  cardType: number
  cardTypeDesc: string
  cardStatus: number
  cardStatusDesc: string
  openCount: number
  qrCode: string
  createdTime: string
}

// 会员
export interface MemberResponse {
  memberId: string
  memberNo: string
  nickname: string
  avatar: string
  memberName: string
  gender: number
  phone: string
  email: string
  levelId: string
  levelName: string
  totalPoints: number
  availablePoints: number
  totalStay: number
  totalNights: number
  totalConsume: number
  balance: number
  status: number
  statusDesc: string
  registerTime: string
  createdTime: string
}

// 订单状态
export const OrderStatus: Record<number, string> = {
  1: '待支付',
  2: '已支付',
  3: '已排房',
  4: '已入住',
  5: '已完成',
  6: '已取消',
  7: '已退款'
}

// 房间状态
export const RoomStatus: Record<number, string> = {
  1: '空房',
  2: '占用',
  3: '维修',
  4: '清洁',
  5: '预订',
  6: '封房'
}