using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using devy.WebGateLib;

namespace AspNetCoreDemo.Filters
{
    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE (ASP.NET Core) - Action Filter 적용 방식
    * ----------------------------------------------------------------------------------------------
    * BACKEND 방식 : 액션 실행 직전(OnActionExecuting)에 유량제어 서비스 호출
    *
    *   ⊙ ASP.NET Core의 ActionFilterAttribute는 MVC Framework의 Action Filter에 대응한다.
    *   ⊙ context.HttpContext 를 WebGate 생성자로 주입한다. (Core는 HttpContext.Current 없음)
    *   ⊙ context.Result 를 설정하면 액션이 단락(short-circuit)되어 응답이 교체된다.
    *   ⊙ 유량제어가 필요한 대문/이벤트 액션에만 [WebGateFilter(...)] 속성으로 선별 적용한다.
    * ==============================================================================================
    */
    public class WebGateFilterAttribute : ActionFilterAttribute
    {
        public string ServiceId { get; set; } = "";
        public string GateId { get; set; } = "";

        public override void OnActionExecuting(ActionExecutingContext context)
        {
            if (context == null)
            {
                return;
            }

            // ServiceId / GateId가 지정된 경우에만 유량제어 동작 (미지정 액션은 통과)
            if (string.IsNullOrEmpty(ServiceId) || string.IsNullOrEmpty(GateId))
            {
                base.OnActionExecuting(context);
                return;
            }

            try
            {
                var webgate = new WebGate(ServiceId, GateId, context.HttpContext);

                // WG_IsNeedToWait() == true → 대기 필요 → 대기UI로 응답 교체
                if (webgate.WG_IsNeedToWait())
                {
                    context.Result = new ContentResult
                    {
                        Content     = webgate.WG_GetWaitingUi(),
                        ContentType = "text/html"
                    };
                }
            }
            catch
            {
                // 유량제어 호출 실패 시 통과(서비스 중단 방지)
            }

            base.OnActionExecuting(context);
        }
    }
}
