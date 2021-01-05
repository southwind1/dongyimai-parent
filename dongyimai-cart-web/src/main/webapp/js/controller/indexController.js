app.controller('indexController',function($scope,loginService){
    //获取当前登录的用户名称
    $scope.showName=function(){
        loginService.showName().success(function(response){
            //取出用户名
            $scope.loginName=response.loginName;


        })



    }



})