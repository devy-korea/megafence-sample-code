using System.Web.Mvc;
using devy.WebGateLib;

namespace AspNetMvc.Filters
{
    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE (ASP.NET MVC) - Action Filter 적용 방식
    * ----------------------------------------------------------------------------------------------
    * BACKEND 방식 : 액션 실행 직전(OnActionExecuting)에 유량제어 서비스 호출
    *
    * <적용 위치 선정 사유>
    *   ⊙ Action Filter는 WebForms의 Page_Load / Java Interceptor에 대응하는 MVC 관용 위치이다.
    *   ⊙ OnActionExecuting은 컨트롤러 액션(무거운 업무로직) 실행 "이전"에 동작한다.
    *   ⊙ filterContext.Result 를 설정하면 액션이 단락(short-circuit)되어,
    *      Response.End()(ThreadAbortException)보다 안전하게 응답을 교체한다.
    *
    * <이용 안내>
    *   ⊙ 유량제어가 필요한 대문/이벤트 액션에만 [WebGateFilter(...)] 속성으로 선별 적용한다.
    *      (전역 필터 등록 시 대상 외 액션까지 동작하므로 권장하지 않음)
    * ==============================================================================================
    */
    public class WebGateFilterAttribute : ActionFilterAttribute
    {
        public string ServiceId { get; set; }
        public string GateId { get; set; }
        public bool IsReplaceMode { get; set; }

        public WebGateFilterAttribute()
        {
            ServiceId     = "9000";
            GateId        = "1";
            IsReplaceMode = true;
        }

        public override void OnActionExecuting(ActionExecutingContext filterContext)
        {
            if (filterContext == null)
            {
                return;
            }

            try
            {
                WebGate webgate = new WebGate(ServiceId, GateId);

                // 대기 필요 시 응답 교체(또는 리다이렉트)로 액션 단락
                if (webgate.WG_IsNeedToWaiting())
                {
                    if (IsReplaceMode)
                    {
                        filterContext.Result = new ContentResult
                        {
                            Content     = webgate.WG_GetWaitingUi(),
                            ContentType = "text/html"
                        };
                    }
                    else
                    {
                        filterContext.Result = new RedirectResult("~/landing.html");
                    }
                    return;
                }
            }
            catch
            {
                // 유량제어 호출 실패 시 통과(서비스 중단 방지)
            }

            base.OnActionExecuting(filterContext);
        }
    }
}
