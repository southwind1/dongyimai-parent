 //控制层 
app.controller('goodsController' ,function($scope,$controller,$location,
										   goodsService,uploadService,itemCatService,
										   typeTemplateService,){
	
	$controller('baseController',{$scope:$scope});//继承
	
    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    
	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	//查询实体 
	$scope.findOne=function(id){
		//接收从goods.html传过来的id
		var id = $location.search()["id"];
		if (id == 0){
			return ;
		}
		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;
			//	定义富文本
				editor.html($scope.entity.goodsDesc.introduction);
			//	定义图片
				$scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
			//	定义扩展属性
				$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);
			//	定义规格
				$scope.entity.goodsDesc.specificationItems = JSON.parse($scope.entity.goodsDesc.specificationItems);
			//	定义sku,解析"{\"网络\":\"移动4G\",\"机身内存\":\"16G\"}”成json格式
			//	循环$scope.entity.itemList
				for (var i = 0;i < $scope.entity.itemList.length;i++){
					$scope.entity.itemList[i].spec = JSON.parse($scope.entity.itemList[i].spec);
				}
			}
		);				
	}
	
	//保存 
	$scope.save=function(){
		//提取富文本的值
		$scope.entity.goodsDesc.introduction = editor.html();
		var serviceObject;//服务层对象  				
		if($scope.entity.goods.id!=null){//如果有ID
			serviceObject=goodsService.update( $scope.entity ); //修改  
		}else{
			serviceObject=goodsService.add( $scope.entity  );//增加 
		}				
		serviceObject.success(
			function(response){
				if(response.success){
					//重新查询 
		        	// $scope.reloadList();//重新加载
					location.href = "goods.html";
				//	清空
					$scope.entity = {};
					editor.html("");
				}else{
					alert(response.message);
				}
			}		
		);				
	}

	//添加Goods
	$scope.add = function(){
		$scope.entity.goodsDesc.introduction = editor.html();
		goodsService.add( $scope.entity  ).success(
			function (response) {
				if (response.success){
					alert(response.message);
				//	清空实体
				// 	$scope.entity = {};
				// 	//清空
					$scope.entity={ goodsDesc:{itemImages:[],specificationItems:[]}  };
					editor.html('');//清空富文本编辑器
				}else {
					alert(response.message);
				}

			}
		)
	}
	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds=[];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	/**
	 * 上传图片
	 */
	$scope.uploadFile=function(){
		uploadService.uploadFile().success(function(response) {
			if(response.success){//如果上传成功，取出url
				$scope.image_entity.url=response.message;//设置文件地址
			}else{
				alert(response.message);
			}
		}).error(function() {
			alert("上传发生错误");
		});
	};
	$scope.entity={goods:{},goodsDesc:{itemImages:[]}};//定义页面实体结构
	//添加图片列表
	$scope.add_image_entity=function(){

		$scope.entity.goodsDesc.itemImages.push($scope.image_entity);
	}

	//列表中移除图片
	$scope.remove_image_entity=function(index){
		$scope.entity.goodsDesc.itemImages.splice(index,1);
	}
// 获取一级目录
	$scope.selectItemCat1List = function () {
		itemCatService.findByParentId(0).success(
			function (response) {
				$scope.itemCat1List = response;
			}
		)
	}
//	获取二级目录,当一级目录的entity.goods.category1Id改变是执行

	$scope.$watch("entity.goods.category1Id",function (newValue,oldValue) {
		itemCatService.findByParentId(newValue).success(
			function (response) {
				$scope.itemCat2List  = response;
			}
		)
	})
	//	获取三级目录,当二级目录的entity.goods.category2Id改变是执行

	$scope.$watch("entity.goods.category2Id",function (newValue,oldValue) {
		itemCatService.findByParentId(newValue).success(
			function (response) {
				$scope.itemCat3List  = response;
			}
		)
	})
	//三级分类选择后  读取模板ID
	$scope.$watch('entity.goods.category3Id', function(newValue, oldValue) {
		//判断三级分类被选中，在去获取更新模板id
		if(newValue){
			itemCatService.findOne(newValue).success(
				function(response){
					$scope.entity.goods.typeTemplateId=response.typeId; //更新模板ID
				}
			);
		}
	});
//	当entity.goods.typeTemplateId改变执行
	$scope.$watch("entity.goods.typeTemplateId",function (newValue,oldValue) {
		typeTemplateService.findOne(newValue).success(
			function (response) {
				$scope.typeTemplate = response;
				//	从后台获取得$scope.typeTemplate.brandIds是json字符串需转换为json格式
				$scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);
			//	将后台获取的扩展属性custom_attribute_items保存到$scope.entity.goodsDesc.customAttributeItems
				if ($location.search()["id"] == null){
					$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);
				}
			}
		);
		//查询规格列表
		typeTemplateService.findSpecList(newValue).success(
			function (response) {
				$scope.specList  = response;
			}
		);
	})
	//[{"attributeName":"网络制式","attributeValue":["移动3G","移动4G"]},{"attributeName":"屏幕尺寸","attributeValue":["5.5寸","5寸"]}]
	$scope.entity={ goodsDesc:{itemImages:[],specificationItems:[]}  };

	$scope.updateSpecAttribute = function ($event,name,value) {
	//	查看选中的规格是否在数据库中已经保存，保存的话就返回这个对象
		var object = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems,"attributeName",name);
		if (object != null){
			//判断勾选没有
			if ($event.target.checked){
				object.attributeValue.push(value);
			}else {
				//	没有勾选的话，删除这个
				//	得到要删除的索引
				object.attributeValue.splice(object.attributeValue.indexOf(value),1);
			//	如果选项都删除的话，就移除这个规格
				if (object.attributeValue.length == 0){
					var index = $scope.entity.goodsDesc.specificationItems.indexOf(object);
					$scope.entity.goodsDesc.specificationItems.splice(index,1);
				}
			}
		}else {
		//	如果数据库中没有这个规格添加这个规格
			$scope.entity.goodsDesc.specificationItems.push({"attributeName":name,"attributeValue" : [value]});
		}

	}

//	显示sku列表
//	实现思路，创建一个集合，循环用户的规格，根据规格名称和规格选项对集合进行扩充，新增的规格数和规格选项的个数相同
	$scope.createItemList = function () {
	//	定义一个空的集合
		$scope.entity.itemList = [{spec:{},price:0,num:99999,status:"0",isDefault:"0"}];
	//	获取规格集合
		var items = $scope.entity.goodsDesc.specificationItems;
	//	循环规格
		for(var i = 0;i < items.length;i++){
			//调用添加行的方法
			$scope.entity.itemList = addColumn($scope.entity.itemList,items[i].attributeName,items[i].attributeValue);
		}
	}
	//添加行
	addColumn = function (list,columnName,conlumnValues) {
	//	创建一个新的集合
		var newList = [];
	//	首先复制老的行，添加新的列，加入的新的集合返回
	//	遍历所有的老行，
		for (var i = 0;i < list.length;i++){
			var oldRow = list[i];
		//	循环所有的要添加的新的规格选项
			for (var j = 0;j < conlumnValues.length;j++){
				//	对老行进行深克隆，这样可以保留老行，可以进行对此克隆,想转换出一个String对象，在解析为JSON格式
				var newRow = JSON.parse(JSON.stringify(oldRow));
			//	在这个新行中添加新的列
				newRow.spec[columnName] = conlumnValues[j];
				newList.push(newRow);
			}

		}
		return newList;
	}


//	定义good.html auditStatus，定义一个状态数组，根据auditStatus作为索引
	$scope.status = ["未审核","已审核","审核未通过","关闭"];
//	显示goods.html 的分类信息，通过一次性查找所有的类型生成一个类型列表，category2Id作为id查找
//	定义一个数组
	$scope.itemCatList = [];
	$scope.findItemCatList = function () {
		itemCatService.findAll().success(
			function (response) {
			//	循环response
				for (var i = 0;i < response.length;i++){
				//	已categoryID作为索引,使用的是关联数组，下标是字符串
					$scope.itemCatList[response[i].id] = response[i].name;
				}
			}
		)
	}
//根据规格名称和选项名称返回是否被勾选

	$scope.checkAttributeValue = function (specName,optionName) {

	//	获取从后台得到的规格
		 var item = $scope.entity.goodsDesc.specificationItems;
	//	 根据关键字,和值获取item json数组中的数组
		var object = $scope.searchObjectByKey(item,"attributeName",specName);
	//	遍历object
		if (object != null){
			//判断object.attributeValue中是否有optionName
			if (object.attributeValue.indexOf(optionName) != -1){

				return true;
			}else {
				return false;
			}
		}else {
			return false;
		}
	}


});	