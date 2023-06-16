<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文件名称" prop="fileName">
        <el-input v-model="queryParams.fileName" placeholder="请输入文件名称" clearable style="width: 240px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类别" prop="fileType">
        <!-- <el-select v-model="queryParams.fileType" placeholder="文件类别" clearable style="width: 240px;padding-right: 25px;">
          <el-option label="1" value="1">1</el-option>
          <el-option label="2" value="2">2</el-option>
          <el-option label="3" value="3">3</el-option>
        </el-select> -->
        <el-input v-model="queryParams.fileType" placeholder="请输入文件类型" clearable style="width: 240px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>

      <el-form-item label="排序方式" prop="sortMode">
        <el-select v-model="queryParams.sortMode" placeholder="按序号排序" clearable style="width: 240px;">
          <el-option label="新的在前" value="New"></el-option>
          <el-option label="旧的在前" value="Old"></el-option>
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
      <!-- <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
          @click="handleDelete">删除</el-button>
      </el-col> -->
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

    <el-table v-loading="loading" :data="fileList" @selection-change="handleSelectionChange">
      <!-- <el-table-column type="selection" width="55" align="center" /> -->

      <el-table-column label="预览" align="center" prop="imgUrl" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <!-- <el-button  type="text" @click="showDetials(scope.row)">
            <span>{{ scope.row.fileName }}</span>
          </el-button> -->
          <!-- <span>{{ scope.row }}</span> -->
          <el-image v-if="scope.row.imgUrl" style="width: 100px; 
          height: 100px" :src="scope.row.imgUrl" fit="cover" :preview-src-list="[scope.row.imgUrl]">
          </el-image>

        </template>
      </el-table-column>

      <el-table-column label="名称" align="center" prop="fileName" :show-overflow-tooltip="true">
        <!-- <template slot-scope="scope">
          <el-button type="text" @click="showDetials(scope.row)">
            <span>{{ scope.row.fileName }}</span>
          </el-button>
        </template> -->
      </el-table-column>
      <!-- <el-table-column
        label="父标签"
        align="center"
        :show-overflow-tooltip="true"
        prop="tagParentName"
      >
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="showDetials({data:scope.row,row:'parent'})">
            <span>{{ scope.row.tagParentName }}</span>
          </el-button>
        </template>
      </el-table-column> -->

      <el-table-column label="类型" align="center" prop="fileType">
        <!--<template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
          <span>{{scope.row.tagClass}}</span>
        </template>-->
      </el-table-column>
      <!-- <el-table-column label="热度" align="center" prop="tagPopularity">
      </el-table-column> -->


      <el-table-column label="创建时间" align="center" prop="createTime"></el-table-column>
      <el-table-column label="最近访问时间" align="center" prop="visitTime"></el-table-column>
      <el-table-column label="文件ID" align="center" prop="fileId"></el-table-column>
      <!-- <el-table-column
        label="备注"
        align="center"
        prop="remark"
        :show-overflow-tooltip="true"
      /> -->
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
    <el-dialog :title="title" :visible.sync="config_open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">

        <el-form-item v-if="(imgTypeList.indexOf(form.fileType) > -1) && (form.imgUrl)" label="文件预览">
          <el-image style="width: 100px; 
          height: 100px" :src="form.imgUrl" fit="cover" :preview-src-list="[form.imgUrl]">
          </el-image>
        </el-form-item>

        <el-form-item v-if="!form.fileId" label="">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择文件
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="form.fileId" label="文件ID" prop="fileId">
          <el-input readonly v-model="form.fileId" />
        </el-form-item>

        <el-form-item label="文件名" prop="fileName">
          <el-input v-model="form.fileName" placeholder="请输入文件名" />
        </el-form-item>

        <el-form-item label="文件类型" prop="fileType">
          <template v-if="form.fileId">
            <el-input readonly v-model="form.fileType" />
          </template>
          <template v-else>
            <el-select v-model="form.fileType" filterable placeholder="文件类型">
              <el-option v-for="(item, index) in imgTypeList.concat(fileTypeList)" :key="index" :value="item">
              </el-option>
            </el-select>
          </template>
        </el-form-item>

        <!-- <el-form-item label="标签级别" prop="tagClass" v-if="!form.tagID">
          <el-radio-group v-model="form.tagClass" size="small"
            @change="handleConfigPageParentTagNameSelectChanged(form.tagClass)">
            <el-radio-button label="1" value="1">级别1</el-radio-button>
            <el-radio-button label="2" value="2">级别2</el-radio-button>
            <el-radio-button label="3" value="3">级别3</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"
            :autosize="{ minRows: 5, maxRows: 15 }"></el-input>
        </el-form-item> -->


      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 显示详情对话框 -->
    <el-dialog :title="title" :visible.sync="detail_open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" readonly />
        </el-form-item>
        <el-form-item label="父标签名" prop="tagParentName">
          <el-input v-model="form.tagParentName" readonly />
        </el-form-item>
        <el-form-item label="标签级别" prop="tagClass">
          <el-input v-model="form.tagClass" readonly />
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-input v-model="form.createTime" readonly />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" readonly :autosize="{ minRows: 5, maxRows: 15 }"></el-input>
        </el-form-item>
      </el-form>
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

import { getFileInfo, getFileBinaryById, getImgUrl, updateFileInfo, uploadFile, delFile } from "@/api/manage/file.js";

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
        fileName: undefined,
        fileType: undefined,
        tagDate: undefined,
        sortMode: "New",
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
      imgTypeList: ["jpg", "jpeg", "png", "svg", "gif", "tif", "bmp", "ico", "webp"],
      fileTypeList: ["txt", "mobi", "epub", "pdf"],
      downloadingUrl: process.env.VUE_APP_BASE_API + "/file/getFileBinaryById/",
      fileList: [],
      tableShow: true
    };
  },
  created() {
    console.log(sessionStorage.getItem("fileShowOrder"))
    if (sessionStorage.getItem("fileShowOrder") === "ok") {
      console.log("使用初始化配置")
      if (sessionStorage.getItem("fileShowOrderParam") === 'Old')
        this.queryParams.sortMode = "Old"
      if (sessionStorage.getItem("fileShowOrderParam") === 'New')
        this.queryParams.sortMode = "New"
    }
    console.log(this.queryParams.sortMode)
    this.getList();
    console.log('继续往下走');
    //if(this.$store.state.tagneedRunPreset === true)
    //  console.log("使用配置")
    console.log(sessionStorage.getItem("tagneedRunPreset"))
    console.log("创建tag页面ing");
  },
  methods: {
    /** 查询标签类型列表 */
    async getList() {
      this.loading = true;
      // listType(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
      //     this.typeList = response.rows;
      //     this.total = response.total;
      //     this.loading = false;
      //   }
      // );
      await getFileInfo(this.queryParams).then((res) => {
        this.fileList = res.data;
        this.total = res.length;
        let promiseArray = []
        for (let i = 0; i < this.fileList.length; ++i) {
          if (this.imgTypeList.indexOf(this.fileList[i].fileType) > -1) {
            promiseArray.push(
              getImgUrl({ fileId: this.fileList[i].fileId }).then((res) => {
                // const imgUrl = window.URL.createObjectURL(new window.Blob([res]), { type: 'image/' + this.fileList[i].fileType });
                // const imgUrl = window.URL.createObjectURL(new window.Blob([res]));
                // this.fileList[i].imgUrl = imgUrl;
                this.$set(this.fileList[i], 'imgUrl', res);
                // console.log("成功获取到了图片数据: " + i);
                // console.log(this.fileList[i].imgUrl);
              }).catch((err) => {
                console.log(err)
                console.log("获取图片" + this.fileList[i].fileName + "失败");
              })
            );
          }
        }

        // console.log("promiseArray");
        // console.log(promiseArray);

        return Promise.all(promiseArray).then(() => {
          this.loading = false;
          // console.log('成功获取了promiseArray');
          // console.log(this.fileList);
          // this.flushTable();
        })
      });
    },
    flushTable() {
      this.tableShow = false;
      this.tableShow = true;
    },
    // 取消按钮
    cancel() {
      this.config_open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        fileName: undefined,
        fileType: undefined,
        fileContent: undefined,
        fileId: undefined,
        imgUrl: require('@/assets/icons/add.png')
      };
      this.resetForm("form");
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
      this.title = "添加标签类型";
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.tagName);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const fileId = row.fileId;
      getFileInfo({ fileId: fileId }).then((res) => {
        this.form = res.data[0];
        getImgUrl({ fileId: fileId }).then((res) => {
          this.form.imgUrl = res;
          this.config_open = true;
          this.title = "修改标签类型";
        })
      });
    },
    showDetials(param) {
      // this.reset();
      // const tagName = param.row === 'parent' ? param.data.tagParentName : param.data.tagName;
      // getTag({ tagName: tagName }).then((res) => {
      //   console.log("点开详情页面，收到数据");
      //   console.log(res);
      //   this.form = res.data[0];
      //   console.log(res.data[0])
      //   this.detail_open = true;
      //   this.title = "标签详情";
      // });

      let curFileId = param.fileId;
      getFileBinaryById({ fileId: curFileId }).then((res) => {

      })

    },

    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.form.fileId != undefined) {
            updateFileInfo(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.config_open = false;
              this.getList();
            });
          } else {
            uploadFile(this.form).then(() => {
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
      // const fileId = row.fileId ? [row.fileId + ""] : this.ids;
      const fileId = row.fileId;
      this.$modal
        .confirm('是否确认删除名称为"' + row.fileName.toString()+" ID为 "+ fileId + '"的数据项？')
        .then(() => {
          // fileId.forEach((item) => {
          //   delFile({ fileId: item })
          //     .then(() => {
          //       this.$modal.msgSuccess('删除成功"' + item + '"');
          //     })
          //     .catch(() => {
          //       this.$modal.msgError('删除文件"' + item + '"失败');
          //     });
          // });
          delFile({ fileId: fileId })
          .then(() => {
            this.$modal.msgSuccess('删除成功"' + row.fileName + '"');
            this.getList();
          })
          .catch(() => {
            this.$modal.msgError('删除文件"' + row.fileName + '"失败');
            this.getList();
          });
        })
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
    handleConfigpageAddFile() {
      alert("hi");
    },
    // 覆盖默认的上传行为
    requestUpload() {
    },
    // 上传预处理
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。");
      } else {
        // console.log("file")
        // console.log(file)
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          // console.log("reader.result");
          // console.log(reader.result);
          this.form.imgUrl = reader.result;
          this.form.fileContent = reader.result.substr(reader.result.indexOf(",")+1, reader.result.length - reader.result.indexOf(","));
          let fileFullName = file.name.trim();
          let fileName = fileFullName.substr(0,fileFullName.lastIndexOf("."));
          console.log("fileName = "+fileName);
          let fileType = fileFullName.substr(fileFullName.lastIndexOf(".") + 1,fileFullName.length - fileFullName.lastIndexOf(".") - 1);
          console.log("fileType = "+fileType);
          this.form.fileName = fileName;
          this.form.fileType = fileType;
        };
      }
    },
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
