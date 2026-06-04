using System.Web.Mvc;

namespace AspNetMvc
{
    public class FilterConfig
    {
        public static void RegisterGlobalFilters(GlobalFilterCollection filters)
        {
            filters.Add(new HandleErrorAttribute());

            /*
             * 메가펜스 유량제어 필터(WebGateFilter)는 전역 등록하지 않는다.
             * 유량제어가 필요한 대문/이벤트 페이지 액션에만 [WebGateFilter] 속성으로 선별 적용한다.
             */
        }
    }
}
