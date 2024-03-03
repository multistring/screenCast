package com.tta.module.network.bean

/**
 * @Author Lee
 * @Date 2022-04-21 14:29
 * @Description  分页列表的基类
 */
open class BaseResponseList<T> {
    private var records: MutableList<T> = mutableListOf()
    private val pages = 0    //总页数
    private val current = 0    //当前页数索引
    private val total = 0      //列表总数
    private val size = 0    //每页数量
    //private var last = false   //是否是最后一页

    fun getRecords(): MutableList<T> {
        return records
    }

    fun setRecords(records: MutableList<T>) {
        this.records = records
    }

    fun isLast(): Boolean {
        return current >= pages
    }

    fun loadMoreEnable(): Boolean {
        return !isLast()
    }

    fun getTotal(): Int{
        return total
    }
}