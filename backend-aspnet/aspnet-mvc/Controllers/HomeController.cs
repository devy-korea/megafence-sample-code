using System.Web.Mvc;
using AspNetMvc.Filters;

namespace AspNetMvc.Controllers
{
    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE (ASP.NET MVC)
    * ----------------------------------------------------------------------------------------------
    * 대문(Index) 액션에 [WebGateFilter]를 부착하여, 페이지 로드 시 유량제어 호출코드가
    * 액션 실행 직전(OnActionExecuting)에 동작하도록 한다. (backend replace 방식)
    * ==============================================================================================
    */
    public class HomeController : Controller
    {
        // 대문 페이지 : 유량제어 적용 (응답교체 방식)
        [WebGateFilter(ServiceId = "9000", GateId = "1", IsReplaceMode = true)]
        public ActionResult Index()
        {
            /*
             * 여기서부터 고객의 Heavy business logic(DB 조회 등)이 시작됩니다.
             * 유량제어 필터를 통과한 경우에만 이 코드가 실행됩니다.
             */
            return View();
        }

        // 유량제어 미적용 페이지 예시 (필터 없음)
        public ActionResult About()
        {
            return View();
        }
    }
}
