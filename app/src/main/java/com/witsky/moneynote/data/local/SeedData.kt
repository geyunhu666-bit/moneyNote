package com.witsky.moneynote.data.local

import com.witsky.moneynote.data.local.entity.AccountType
import com.witsky.moneynote.data.local.entity.CategoryKind

/**
 * 首次建库时写入的预置数据。
 *
 * 用原始 SQL 而不是 DAO：建库回调发生在数据库刚创建、DAO 尚不可安全调度协程的时刻，
 * 同步的 execSQL 是唯一没有竞态的选择。语句全部由下面的常量生成，不含用户输入。
 */
internal object SeedData {

  private data class CategorySeed(
    val id: Long,
    val name: String,
    val kind: CategoryKind,
    val parentId: Long?,
    val icon: String,
  )

  private data class AccountSeed(
    val id: Long,
    val name: String,
    val type: AccountType,
    val icon: String,
  )

  private val categories: List<CategorySeed> = buildList {
    // 一级分类
    add(CategorySeed(1, "餐饮", CategoryKind.EXPENSE, null, "🍚"))
    add(CategorySeed(2, "交通", CategoryKind.EXPENSE, null, "🚌"))
    add(CategorySeed(3, "购物", CategoryKind.EXPENSE, null, "🛍️"))
    add(CategorySeed(4, "居住", CategoryKind.EXPENSE, null, "🏠"))
    add(CategorySeed(5, "娱乐", CategoryKind.EXPENSE, null, "🎬"))
    add(CategorySeed(6, "医疗", CategoryKind.EXPENSE, null, "💊"))
    add(CategorySeed(7, "学习", CategoryKind.EXPENSE, null, "📚"))
    add(CategorySeed(8, "人情", CategoryKind.EXPENSE, null, "🧧"))
    add(CategorySeed(9, "其他支出", CategoryKind.EXPENSE, null, "📦"))
    add(CategorySeed(10, "工资", CategoryKind.INCOME, null, "💰"))
    add(CategorySeed(11, "奖金", CategoryKind.INCOME, null, "🎊"))
    add(CategorySeed(12, "理财收益", CategoryKind.INCOME, null, "📈"))
    add(CategorySeed(13, "红包收入", CategoryKind.INCOME, null, "🧧"))
    add(CategorySeed(14, "报销", CategoryKind.INCOME, null, "🧾"))
    add(CategorySeed(15, "其他收入", CategoryKind.INCOME, null, "📥"))

    // 二级分类
    add(CategorySeed(101, "早餐", CategoryKind.EXPENSE, 1, "🍳"))
    add(CategorySeed(102, "午餐", CategoryKind.EXPENSE, 1, "🍜"))
    add(CategorySeed(103, "晚餐", CategoryKind.EXPENSE, 1, "🍲"))
    add(CategorySeed(104, "外卖", CategoryKind.EXPENSE, 1, "🥡"))
    add(CategorySeed(105, "零食饮料", CategoryKind.EXPENSE, 1, "🧋"))

    add(CategorySeed(111, "公交地铁", CategoryKind.EXPENSE, 2, "🚇"))
    add(CategorySeed(112, "打车", CategoryKind.EXPENSE, 2, "🚕"))
    add(CategorySeed(113, "加油", CategoryKind.EXPENSE, 2, "⛽"))
    add(CategorySeed(114, "停车", CategoryKind.EXPENSE, 2, "🅿️"))

    add(CategorySeed(121, "日用品", CategoryKind.EXPENSE, 3, "🧻"))
    add(CategorySeed(122, "服饰", CategoryKind.EXPENSE, 3, "👕"))
    add(CategorySeed(123, "数码", CategoryKind.EXPENSE, 3, "📱"))

    add(CategorySeed(131, "房租", CategoryKind.EXPENSE, 4, "🔑"))
    add(CategorySeed(132, "水电燃气", CategoryKind.EXPENSE, 4, "💡"))
    add(CategorySeed(133, "物业", CategoryKind.EXPENSE, 4, "🏢"))
    add(CategorySeed(134, "宽带话费", CategoryKind.EXPENSE, 4, "🌐"))

    add(CategorySeed(141, "电影", CategoryKind.EXPENSE, 5, "🎥"))
    add(CategorySeed(142, "游戏", CategoryKind.EXPENSE, 5, "🎮"))
    add(CategorySeed(143, "旅游", CategoryKind.EXPENSE, 5, "✈️"))

    add(CategorySeed(151, "药品", CategoryKind.EXPENSE, 6, "💊"))
    add(CategorySeed(152, "门诊", CategoryKind.EXPENSE, 6, "🏥"))

    add(CategorySeed(161, "书籍", CategoryKind.EXPENSE, 7, "📖"))
    add(CategorySeed(162, "课程", CategoryKind.EXPENSE, 7, "🎓"))

    add(CategorySeed(171, "随礼红包", CategoryKind.EXPENSE, 8, "🧧"))
    add(CategorySeed(172, "请客", CategoryKind.EXPENSE, 8, "🍻"))

    add(CategorySeed(181, "其他", CategoryKind.EXPENSE, 9, "📦"))
  }

  private val accounts: List<AccountSeed> = listOf(
    AccountSeed(1, "现金", AccountType.CASH, "💵"),
    AccountSeed(2, "银行卡", AccountType.DEBIT, "🏦"),
    AccountSeed(3, "支付宝", AccountType.EWALLET, "📘"),
    AccountSeed(4, "微信", AccountType.EWALLET, "💬"),
    AccountSeed(5, "信用卡", AccountType.CREDIT, "💳"),
  )

  fun statements(): List<String> {
    val parentOrder = categories.filter { it.parentId == null }
      .mapIndexed { index, seed -> seed.id to index + 1 }.toMap()
    val childOrder = categories.filter { it.parentId != null }
      .groupBy { it.parentId }
      .flatMap { (_, siblings) -> siblings.mapIndexed { index, seed -> seed.id to index + 1 } }
      .toMap()

    val categorySql = categories.map { seed ->
      val order = parentOrder[seed.id] ?: childOrder[seed.id] ?: 0
      val parent = seed.parentId?.toString() ?: "NULL"
      "INSERT INTO category (id, name, kind, parentId, icon, sortOrder, isSystem) " +
        "VALUES (${seed.id}, '${seed.name}', '${seed.kind.name}', $parent, '${seed.icon}', $order, 1)"
    }

    val accountSql = accounts.mapIndexed { index, seed ->
      "INSERT INTO account (id, name, type, initialBalance, icon, includeInNetWorth, isArchived, sortOrder) " +
        "VALUES (${seed.id}, '${seed.name}', '${seed.type.name}', 0, '${seed.icon}', 1, 0, ${index + 1})"
    }

    return categorySql + accountSql
  }
}
