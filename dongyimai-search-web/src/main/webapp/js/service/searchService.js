app.service("searchService",function ($http) {
    /**
     * 根据查询条件查询数据
     */
    this.search = function (searchMap) {
       return $http.post("../itemsearch/search.do",searchMap);
    }
})