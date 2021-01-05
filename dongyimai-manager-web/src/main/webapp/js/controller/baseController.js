app.controller("baseController",function ($scope) {
    //	定义重新加载方法
    $scope.reloadList = function () {
        $scope.selectIds = [];
        /*切换页码*/
        $scope.search($scope.paginationConf.currentPage, $scope.paginationConf.itemsPerPage);
    }
    //	定义分页控制配件
    $scope.paginationConf = {
        currentPage: 1,//当前页
        totalItems: 10,//总的条数
        itemsPerPage: 10,//每页条数
        perPageOptions: [10, 20, 30, 40, 50],//可选每页条数数组
        onChange: function () {
            $scope.reloadList();//重新加载
        }
    };

    //	删除选中的id集合
    $scope.selectIds = [];
    //	更新复选
    $scope.updateSelection = function ($event, id) {
        if ($event.target.checked) {
            $scope.selectIds.push(id);
        } else {
            var idx = $scope.selectIds.indexOf(id);
            $scope.selectIds.splice(idx, 1);
        }
    }
    //提取json字符串数据中某个属性，返回拼接字符串 逗号分隔
    //注意：转换json字符串有可能为空，可以利用if(!jsonString) 判断是否为空
    $scope.jsonToString = function(jsonString,key){
        var json=JSON.parse(jsonString);//将json字符串转换为json对象
        var value="";
        for(var i=0;i<json.length;i++){
            if(i>0){
                value+=","
            }
            value+=json[i][key];
        }
        return value;
    }
})