package com.witsky.moneynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 收支分类，支持二级。[parentId] 为 null 表示一级分类。
 *
 * 这里故意不加自引用外键：删父分类时级联规则会让"父分类下是否还有子分类"的判断变得不直观，
 * 删除安全性交给 repository 用 [com.witsky.moneynote.data.local.dao.CategoryDao] 的计数查询把关。
 */
@Entity(tableName = "category", indices = [Index("parentId"), Index("kind")])
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val kind: CategoryKind,
  val parentId: Long? = null,
  val icon: String = "📦",
  val sortOrder: Int = 0,
  /** 预置分类不可删除，只可改名。 */
  val isSystem: Boolean = false,
)

/** 把扁平分类列表组装成"一级 → 子分类"的两层结构，供选择器使用。 */
fun List<CategoryEntity>.toCategoryTree(kind: CategoryKind): List<CategoryGroup> {
  val scoped = filter { it.kind == kind }
  val parents = scoped.filter { it.parentId == null }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
  return parents.map { parent ->
    CategoryGroup(
      parent = parent,
      children = scoped.filter { it.parentId == parent.id }.sortedWith(compareBy({ it.sortOrder }, { it.id })),
    )
  }
}

data class CategoryGroup(val parent: CategoryEntity, val children: List<CategoryEntity>)

/** 分类管理页要的是"一行一条"的扁平列表，同时知道这行是不是子分类，用来做缩进。 */
data class CategoryRow(val category: CategoryEntity, val isChild: Boolean)

fun List<CategoryEntity>.toCategoryRows(kind: CategoryKind): List<CategoryRow> =
  toCategoryTree(kind).flatMap { group ->
    listOf(CategoryRow(group.parent, isChild = false)) +
      group.children.map { CategoryRow(it, isChild = true) }
  }
