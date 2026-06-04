using Microsoft.AspNetCore.Mvc;
using AspNetCoreDemo.Filters;

namespace AspNetCoreDemo.Controllers
{
    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE (ASP.NET Core) - ToTheMoon 한정판매 마이크로사이트
    * ----------------------------------------------------------------------------------------------
    * 달나라 로켓여행 패키지 선착순 한정 판매 마이크로사이트.
    * 트래픽이 몰리는 [홈(대문)]과 [구매] 두 페이지에만 동일 GateId로 유량제어를 적용한다.
    *   - 적용 위치 : 액션에 [WebGateFilter] 부착 (OnActionExecuting)
    *   - 미적용 페이지(패키지/일정/FAQ)는 필터 없이 바로 렌더링
    * ==============================================================================================
    */
    public class HomeController : Controller
    {
        // [대문] 이벤트 랜딩 - 유량제어 적용
        [WebGateFilter(ServiceId = "9000", GateId = "1")]
        public IActionResult Index()
        {
            return View();
        }

        // [패키지 소개] - 유량제어 미적용
        public IActionResult Package()
        {
            return View();
        }

        // [여행 일정] - 유량제어 미적용
        public IActionResult Itinerary()
        {
            return View();
        }

        // [FAQ] - 유량제어 미적용
        public IActionResult Faq()
        {
            return View();
        }

        // [구매/예약] 선착순 한정 구매 - 유량제어 적용 (홈과 동일 GateId)
        [WebGateFilter(ServiceId = "9000", GateId = "1")]
        public IActionResult Purchase()
        {
            return View();
        }
    }
}
