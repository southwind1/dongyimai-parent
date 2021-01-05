app.controller("searchController",function ($scope,$location,searchService) {
//    初始化查询空间
    $scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'',
        'pageNo':1,'pageSize':10,'sortField':'','sort':'' };//搜索对象

    //加载查询字符串
    $scope.loadkeywords=function(){
        $scope.searchMap.keywords=  $location.search()['keywords'];
        $scope.search();
    }

    $scope.search = function () {
        //前端重新定义后值为字符串
        $scope.searchMap.pageNo= parseInt($scope.searchMap.pageNo) ;
        searchService.search($scope.searchMap).success(
            function (response) {
                $scope.resultMap = response;
            //    调用分页标签
                buildPageLabel();
            }
        )
    }
//    构建分页标签(totalPages为总页数)
    buildPageLabel = function(){
    //    定义分页标签
        $scope.pageLabel=[];
    //    获取总页数
        var maxPageNo = $scope.resultMap.totalPages;
        //开始页码
        var firstPage = 1;
    //    最后页码
        var lastPage=maxPageNo;

        $scope.firstDot=true;//前面有点
        $scope.lastDot=true;//后边有点

    //    如果总页数大于5页,显示部分页码
        if (maxPageNo > 5){
            //如果当前页小于等于3
            if($scope.searchMap.pageNo<=3){
                lastPage = 5;
                $scope.firstDot=false;//前面没点
            }else if ($scope.searchMap.pageNo>=lastPage-2){//如果当前页大于等于最大页码-2
                //后5页
                firstPage = lastPage - 5;
                $scope.lastDot=false;//后边没点
            }else {
                //显示当前页为中心的5页
                firstPage = $scope.searchMap.pageNo - 2;
                lastPage = $scope.searchMap.pageNo + 2;
            }
        }else {
        //    总页数小于5都没点
            $scope.firstDot=false;//前面没点
            $scope.lastDot=false;//后边没点
        }
        //循环产生页码标签
        for(var i = firstPage;i <= lastPage;i++){
            $scope.pageLabel.push(i);
        }
    }

//    点击页码后，进行页码验证
    $scope.queryByPage = function(pageNo){
    //    页码验证
        if (pageNo < 1 || pageNo > $scope.resultMap.totalPages){
            //违法
            return;
        }else {
            $scope.searchMap.pageNo = pageNo;
            $scope.search();
        }
    }

//    页码为一时上一页不可用
    $scope.isTopPage=function(){
        if ($scope.searchMap.pageNo == 1){
            return true;
        }else {
            return false;
        }
    }

    //    页码为最后时下一页不可用
    $scope.isEndPage=function(){
        if ($scope.searchMap.pageNo == $scope.resultMap.totalPages){
            return true;
        }else {
            return false;
        }
    }
    //判断指定页码是否是当前页
    $scope.ispage=function (p) {
        if(parseInt(p)==parseInt($scope.searchMap.pageNo)){
            return true;
        }else {
            return false;
        }
    }

//添加搜索项
    $scope.addSearchItem=function(key,value){
        //搜索项改变查询页变成1
        $scope.searchMap.pageNo=1;
        if(key=='category' || key=='brand' || key == 'price'){//如果点击的是分类或者是品牌
            $scope.searchMap[key]=value;
        }else{
            $scope.searchMap.spec[key]=value;
        }
        //搜索项每改变一下，重新查一下
        $scope.search();//执行搜索
    }
    //移除复合搜索条件
    $scope.removeSearchItem=function(key){
        //搜索项改变查询页变成1
        $scope.searchMap.pageNo=1;
        if(key=="category" ||  key=="brand" || key == 'price'){//如果是分类或品牌
            $scope.searchMap[key]="";
        }else{//否则是规格
            delete $scope.searchMap.spec[key];//移除此属性
        }
        //搜索项每改变一下，重新查一下
        $scope.search();//执行搜索
    }


    //设置排序规则
    $scope.sortSearch=function(sortField,sort){
        $scope.searchMap.sortField=sortField;
        $scope.searchMap.sort=sort;
        $scope.search();
    }

//    判断关键字中是否有商标
    $scope.keywordsIsBrand = function(){
    //    取出商标
        for (var i = 0;i < $scope.resultMap.brandList.length;i++){
            if ($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text) >= 0){
                return true;
            }
        }
        return false;
    }
})