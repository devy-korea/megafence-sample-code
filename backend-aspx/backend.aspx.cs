using System;
using System.Collections.Generic;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using devy.WebGateLib;

namespace Demo
{

    /*
    * ==============================================================================================
    * 메가펜스 유량제어서비스 SAMPLE(ASPX) V.21.1.0
    * 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    * 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
    * 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
    * 작성자 : ysd@devy.co.kr
    * ----------------------------------------------------------------------------------------------
    *
    * BACKEND 방식 : 페이지 로드 시 Backend 코드(java,jsp,php,aspx 등..)로 유량제어 서비스 호출
    *
    * <이용 안내> 
    *   ⊙ 아래의 샘플코드를 그대로 테스트용 페이지에 삽입해서 대기UI가 표시되는지 확인
    *   ⊙ 서비스 세팅이 완료되면 안내받은 GATE-ID, SERVICE-ID로 수정해서 사용
    *   ⊙ 유량제어 코드는 DB접속 등의 부하량이 많은 업무로직 이전에 삽입해야 효과적입니다.
    *   ⊙ 쿠키나 세션 등을 이용하는 간단한 처리는 유량제어 코드 이전에 배치되어도 무방합니다.
    *
    * <주의 사항>
    *   ⊙ 공통모듈(WebGateLib.cs)을 사용하므로 함께 배포해 주세요. 
    * ==============================================================================================-->
    */

    public partial class backend : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {

            /* 
            고객사 Light business logic ....
                예) 로그인 체크 : 쿠키나 세션을 체크해서 Login 페이지로 redirect
            */


            /* BEGIN OF 유량제어 코드삽입
            ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  */
            string  serviceId   = "9000";  // 할당받은 SERVICE ID (fixed)
            string  gateId      = "1";     // 사용할 GATE ID (할당받은 GATE ID 범위내에서 사용) 
            WebGate webgate     = new WebGate(serviceId, gateId);
            if (webgate.WG_IsNeedToWaiting())
            {
                try
                {
                    // 대기가 필요하면 대기UI로 응답 대체 후 종료
                    Response.Clear();
                    Response.Write(webgate.WG_GetWaitingUi());
                    Response.Flush();
                    Response.End(); 
                }
                catch { }
                finally { }
            }
            /*▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
            END OF 유량제어 코드삽입 */


            /* 
            고객사 Heavy business logic ....
                예) DB 조회 등을 동반하는 처리 (회원정보 조회 등...)
            */
        }
    }
}