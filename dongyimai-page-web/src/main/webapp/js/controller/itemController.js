//商品详细页（控制层）
app.controller("itemController",function ($scope,$http) {
//    数量操作
    $scope.addNum = function (x) {
        $scope.num = $scope.num + x;
        //控制数量不能小于1
        if ($scope.num < 1) {
            $scope.num = 1;
        }
    }
    $scope.specificationItems = {};//记录用户选择的规格
    //用户选择规格
    $scope.selectSpecification = function (name, value) {
        $scope.specificationItems[name] = value;
        //每次specificationItems改变都要读取sku
        searchSku();//读取sku
    }

    //判断某规格选项是否被用户选中
    $scope.isSelected = function (name, value) {
        if ($scope.specificationItems[name] == value) {
            return true;
        } else {
            return false;
        }
    }

//    取的默认商品的sku信息
    $scope.loadSku = function () {
        //    商品的sku列表已近在查询时进行排序啦，第一个就是
        $scope.sku = skuList[0];
        //进行深克隆，毕竟在前端选着sku时$scope.specificationItems会经常变化，如果直接赋值specificationItems指向的
        //是原先的默认sku，我们不想改变skuList的值，所以进行深克隆
        $scope.specificationItems = JSON.parse(JSON.stringify($scope.sku.spec));
    }

//    创建一个从skuList搜索页面选中的sku的方法
    searchSku = function () {
        //循环skuList，找的匹配页面选中的sku
        for (var i = 0;i < skuList.length;i++){
            if (matchObject(skuList[i].spec, $scope.specificationItems)){
            //    相等
                $scope.sku = skuList[i];
                return;
            }
        }
        //如果没有匹配的
        $scope.sku = {
            id:0,
            title:'--------',
            price:0
        }
    }
//    创建一个匹配方法
    matchObject = function (map1,map2) {
        //for in的方遍历集合
        for (var k in map1){
            if (map1[k] != map2[k]){
                return false;
            }
        }
        for (var k in map2){
            if (map2[k] != map1[k]){
                return false;
            }
        }
        return true;
    }
    //加入购物车
    $scope.addToCart  =function () {
        $http.get('http://localhost:9114/cart/addGoodsToCartList.do?itemId='
            + $scope.sku.id +'&num='+$scope.num,{'withCredentials':true}).success(
            function(response){
                if(response.success){
                    location.href='http://localhost:9114/cart.html';//跳转到购物车页面
                }else{
                    alert(response.message);
                }
            }
        );
    }
})