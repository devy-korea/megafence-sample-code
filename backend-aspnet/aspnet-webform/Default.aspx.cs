using devy.WebGateLib;
using System;
using System.Web.UI;

namespace AspNetWebForm
{
    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE (ASP.NET WebForm)
    * ----------------------------------------------------------------------------------------------
    * BACKEND 방식 : 대문 페이지 로드(Page_Load) 시 Backend 코드로 유량제어 서비스 호출
    *
    * <이용 안내>
    *   ⊙ 유량제어 코드는 DB접속 등 부하가 큰 업무로직 "이전"에 삽입해야 효과적입니다.
    *   ⊙ 서비스 세팅 완료 후 안내받은 SERVICE-ID, GATE-ID로 수정하세요.
    *   ⊙ 공통모듈(WebGateLib.cs)을 사용하므로 함께 배포해야 합니다.
    * ==============================================================================================
    */
    public partial class Default : Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {
            /*
             * 고객사 Light business logic ....
             *   예) 쿠키/세션 기반의 간단한 로그인 체크 등 (유량제어 이전 배치 가능)
             */

            /* BEGIN OF 유량제어 코드삽입
            ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
            string serviceId = "9000"; // 할당받은 SERVICE ID로 수정
            string gateId    = "1";    // 사용할 GATE ID로 수정

            try
            {
                WebGate webgate = new WebGate(serviceId, gateId);

                // WG_IsNeedToWaiting() == true → 대기 필요 → 대기UI 표시
                Response.Clear();
                Response.ContentType = "text/html";
                Response.Write(webgate.WG_GetWaitingUi());
                Response.Flush();
                Response.End();
            }
            catch
            {
                // 유량제어 호출 실패 시 통과(서비스 중단 방지)
            }
            /* ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
            END OF 유량제어 코드삽입 */

            /*
             * 고객사 Heavy business logic ....
             *   예) DB 조회 등을 동반하는 처리 (회원정보 조회 등...)
             *   유량제어를 통과한 경우에만 실행됩니다.
             */
        }
    }
}
