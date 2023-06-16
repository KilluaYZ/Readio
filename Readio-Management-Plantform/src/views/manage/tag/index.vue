<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标签内容" prop="content">
        <el-input v-model="queryParams.content" placeholder="请输入标签内容" clearable style="width: 240px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <!-- <el-form-item label="级别" prop="tagClass">
        <el-select
          v-model="queryParams.tagClass"
          placeholder="标签级别"
          clearable
          style="width: 240px;padding-right: 25px;"
        >
          <el-option label="1" value="1">1</el-option>
          <el-option label="2" value="2">2</el-option>
          <el-option label="3" value="3">3</el-option>
        </el-select>
      </el-form-item> -->



      <el-form-item label="排序方式" prop="sortMode">
        <el-select v-model="queryParams.sortMode" placeholder="按序号排序" clearable style="width: 240px;">
          <el-option label="默认排序" value="Default"></el-option>
          <el-option label="按热度排序" value="Hot"></el-option>
          <el-option label="按时间排序" value="New"></el-option>

        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
          @click="handleDelete">删除</el-button>
      </el-col>
      <!-- <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          >导出</el-button
        >
      </el-col> -->
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-refresh" size="mini" @click="handleRefreshCache">刷新缓存</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tagTableData" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />

      <el-table-column label="ID" align="center" prop="tagId" :show-overflow-tooltip="true" />

      <el-table-column label="内容" align="center" prop="content" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <el-button type="text" @click="showDetials(scope.row)">
            <span>{{ scope.row.content }}</span>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="被引用数" align="center" prop="linkedTimes">
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime"></el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color: crimson;"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改参数配置对话框 -->
    <el-dialog :title="title" :visible.sync="config_open" :width="configPageWidth" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="form.tagId" label="ID" prop="tagId">
          <el-input v-model="form.tagId" readonly />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" placeholder="请输入标签内容" />
        </el-form-item>

        <el-form-item v-if="form.tagId" label="创建时间" prop="createTime">
          <el-input v-model="form.createTime" readonly />
        </el-form-item>

        <el-form-item v-if="form.tagId" label="引用情况" >
          <!-- <el-row><el-input v-model="form.createTime" readonly /></el-row> -->
          <el-row>
            <template>
              <el-table v-loading="configPageTableLoading" :data="tagSeriesList" stripe style="width: 100%">
                <el-table-column prop="seriesName" label="名称" width="180">
                </el-table-column>
                <el-table-column v-if="!isShowDetail" label="操作" align="center" class-name="small-padding fixed-width">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-delete" style="color: crimson;"
                      @click="handleTagSeriesDelete(form.tagId, scope.row.seriesId)">删除</el-button>
                  </template>
                </el-table-column>
                <el-table-column type="expand">
                  <template slot-scope="props">
                    <el-form label-position="left">
                      <el-form-item label="系列ID">
                        <span>{{ props.row.seriesId }}</span>
                      </el-form-item>
                      <el-form-item label="系列名">
                        <span>{{ props.row.seriesName }}</span>
                      </el-form-item>
                      <el-form-item label="作者 ID">
                        <span>{{ props.row.userId}}</span>
                      </el-form-item>
                      <el-form-item label="作者名">
                        <span>{{ props.row.userName }}</span>
                      </el-form-item>
                      <el-form-item label="状态">
                        <span v-if="props.row.isFinished">已完结</span>
                        <span v-else>未完结</span>
                      </el-form-item>
                      <el-form-item label="系列简介">
                        <!-- <el-input v-model="props.row.abstract" type="textarea" readonly
                          :autosize="{ minRows: 5, maxRows: 15 }">
                        </el-input> -->
                        <span>{{ props.row.abstract }}</span>
                      </el-form-item>
                      <el-form-item label="创建时间">
                        <span>{{ props.row.createTime }}</span>
                      </el-form-item>
                      <el-form-item label="喜欢数">
                        <span>{{ props.row.likes }}</span>
                      </el-form-item>
                      <el-form-item label="浏览数">
                        <span>{{ props.row.views }}</span>
                      </el-form-item>
                      <el-form-item label="分享数">
                        <span>{{ props.row.shares }}</span>
                      </el-form-item>
                      <el-form-item label="收藏数">
                        <span>{{ props.row.collect }}</span>
                      </el-form-item>
                    </el-form>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </el-row>
        </el-form-item>

        <!-- <el-form-item label="父标签名" prop="tagParentName">
          <el-select
            v-model="form.tagParentName"
            filterable
            placeholder="父标签名"
            :disabled="configPageParentTagDisabled"
          >
            <el-option
              v-for="item in configPageParentTags"
              :key="item.tagName"
              :value="item.tagName"
            >
            </el-option>
          </el-select>
        </el-form-item> -->
        <!-- <el-form-item label="标签级别" prop="tagClass" v-if="!form.tagID">
          <el-radio-group
            v-model="form.tagClass"
            size="small"
            @change="handleConfigPageParentTagNameSelectChanged(form.tagClass)"
          >
            <el-radio-button label="1" value="1">级别1</el-radio-button>
            <el-radio-button label="2" value="2">级别2</el-radio-button>
            <el-radio-button label="3" value="3">级别3</el-radio-button>
          </el-radio-group>
        </el-form-item> -->
        <!-- <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="请输入内容"
            :autosize="{ minRows: 5, maxRows: 15 }"
          ></el-input>
        </el-form-item> -->
      </el-form>
      <div slot="footer" class="dialog-footer" v-if="!isShowDetail">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>

    </el-dialog>

  </div>
</template>

<script>
// import {
//   listType,
//   getType,
//   delType,
//   addType,
//   updateType,
//   refreshCache,
// } from "@/api/system/dict/type";

import { addTag, delTag, updateTag, getTag, getAllTagSeries, delTagSeriesRelation } from "@/api/manage/tag.js";

export default {
  name: "Tag",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 标签表格数据
      tagTableData: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      config_open: false,
      // 是否显示详情页面
      detail_open: false,
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        content: undefined,
        tagId: undefined,
        sortMode: "Default",
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        tagName: [
          { required: true, message: "标签名称不能为空", trigger: "blur" },
        ],
        tagClass: [
          { required: true, message: "标签级别不能为空", trigger: "blur" },
        ],
      },
      configPageParentTags: [],
      configPageParentTagDisabled: false,
      isShowDetail: false,
      tagSeriesList: [],
      configPageTableLoading: false,
      configPageWidth: "500px"
    };
  },
  created() {
    console.log(sessionStorage.getItem("tagneedRunPreset"))
    if (sessionStorage.getItem("tagneedRunPreset") === "ok") {
      console.log("使用初始化配置")
      if (sessionStorage.getItem("tagpresetParam") === 'Hot')
        this.queryParams.sortMode = "Hot"
      if (sessionStorage.getItem("tagpresetParam") === 'New')
        this.queryParams.sortMode = "New"
    }
    console.log(this.queryParams.sortMode)
    this.getList();
    //if(this.$store.state.tagneedRunPreset === true)
    //  console.log("使用配置")
    console.log(sessionStorage.getItem("tagneedRunPreset"))
    console.log("创建tag页面ing");
  },
  methods: {
    /** 查询标签类型列表 */
    getList() {
      this.loading = true;
      // listType(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
      //     this.typeList = response.rows;
      //     this.total = response.total;
      //     this.loading = false;
      //   }
      // );
      getTag(this.queryParams).then((res) => {
        this.tagTableData = res.data;
        this.total = res.length;
        this.loading = false;
      });
    },

    // 取消按钮
    cancel() {
      this.config_open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        content: undefined,
        tagId: undefined,
        createTime: undefined,
        linkedTimes: undefined,
      };
      this.resetForm("form");

      this.tagSeriesList = [];
      this.isShowDetail = false;
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.config_open = true;
      this.title = "添加标签";
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.tagId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const tagId = row.tagId;
      getTag({ tagId: tagId }).then((res) => {
        this.form = res.data[0];
        // this.handleConfigPageParentTagNameSelectChanged(this.form.tagClass)

        getAllTagSeries({tagId: tagId}).then((res) => {
          this.tagSeriesList = res.data;
          this.configPageWidth = "800px";
          this.config_open = true;
          this.title = "修改标签";
        });
        
      });
    },
    showDetials(row) {
      this.reset();
      const tagId = row.tagId;
      getTag({ tagId: tagId }).then((res) => {
        this.form = res.data[0];
        getAllTagSeries({tagId: tagId}).then((res) => {
          this.tagSeriesList = res.data;
          this.config_open = true;
          this.isShowDetail = true;
          this.title = "查看标签详情";
        });
        
      });
    },

    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.form.tagId != undefined) {
            updateTag(this.form).then((response) => {
              this.$modal.msgSuccess("修改成功");
              this.config_open = false;
              this.getList();
            });
          } else {
            addTag(this.form).then((response) => {
              this.$modal.msgSuccess("新增成功");
              this.config_open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.loading = true;
      const tagId = row.tagId ? [row.tagId + ""] : this.ids;
      var promiseList = [];
      this.$modal
        .confirm('是否确认删除标签名称为"' + tagId.toString() + '"的数据项？')
        .then(() => {
          tagId.forEach((item) => {
            promiseList.push(delTag({ tagId: item })
              .then(() => {
                this.$modal.msgSuccess('成功删除标签"' + item + '"');
              })
              .catch(() => {
                this.$modal.msgError('删除标签"' + item + '"失败');
              }));
          });
          
        }).then(() => {
          Promise.all(promiseList).then(() => {
            this.getList();
          })
        })
        .catch(() => { this.loading = false; });
        
    },
    /** 导出按钮操作 */
    handleExport() {
      // this.download(
      //   "system/dict/type/export",
      //   {
      //     ...this.queryParams,
      //   },
      //   `type_${new Date().getTime()}.xlsx`
      // );
      alert("暂不支持该功能");
    },
    /** 刷新缓存按钮操作 */
    handleRefreshCache() {
      refreshCache().then(() => {
        this.$modal.msgSuccess("刷新成功");
        this.$store.dispatch("dict/cleanDict");
      });
    },
    handleConfigPageParentTagNameSelectChanged(selectedTagClass) {
      if (selectedTagClass == 1) {
        //选了级别1时，让父标签失效
        this.configPageParentTagDisabled = true;
      } else {
        this.configPageParentTagDisabled = false;
        getTag({ tagClass: selectedTagClass - 1 }).then((res) => {
          this.configPageParentTags = res.data;
        });
      }
    },
    //处理删除tag对应的series的事件
    handleTagSeriesDelete(tagId, seriesId) {
      this.configPageTableLoading = true;
      delTagSeriesRelation({tagId: tagId, seriesId: seriesId}).then(() => {
        this.$modal.msgSuccess("删除成功");
        getAllTagSeries({tagId: tagId}).then((res) => {
          this.tagSeriesList = res.data;
          this.configPageTableLoading = false;
        });
        
      }).catch(() => {
        this.$modal.msgError("删除失败");
        getAllTagSeries({tagId: tagId}).then((res) => {
          this.tagSeriesList = res.data;
          this.configPageTableLoading = false;
        });
      })
    }
  },
  watch: {
    queryParams: {
      handler(newQuery, oldQuery) {
        console.log("newQuery");
        console.log(newQuery);
      },
      immediate: true,
      deep: true,
    },
  },
};
</script>
