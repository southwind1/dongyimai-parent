var app = angular.module("dongyimai",[]);//定义一个叫myapp的模块,增加分页插件

/*$sce服务写成过滤器*/
app.filter("trustHtml",["$sce",function ($sce) {
    return function (data) {
        return $sce.trustAsHtml(data);
    }
}])