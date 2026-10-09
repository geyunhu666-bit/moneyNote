package com.witsky.moneynote.data.local.entity

/** 账户类型。仅影响展示归类，不参与余额计算。 */
enum class AccountType(val label: String, val icon: String) {
  CASH("现金", "💵"),
  DEBIT("储蓄卡", "🏦"),
  CREDIT("信用卡", "💳"),
  EWALLET("电子钱包", "📱"),
  INVEST("投资", "📈"),
}

/** 分类只分收支两类，与账目类型对应（转账不属于任何分类）。 */
enum class CategoryKind(val label: String) {
  EXPENSE("支出"),
  INCOME("收入"),
}

enum class TransactionType(val label: String) {
  EXPENSE("支出"),
  INCOME("收入"),
  TRANSFER("转账"),
}
