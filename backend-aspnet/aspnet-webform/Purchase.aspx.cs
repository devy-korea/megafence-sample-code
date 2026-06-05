using devy.WebGateLib;
using System;
using System.Web.UI;

namespace AspNetWebForm
{
    /*
    * ==============================================================================================
    * [구매/예약] 페이지 - 유량제어 적용 (홈/대문과 동일 GateId)
    * ----------------------------------------------------------------------------------------------
    * 선착순 결제가 몰리는 구매 페이지도 대문과 같은 GateId로 묶어, 동일 대기열로 제어한다.
    * 페이지 로드(Page_Load) 시 유량제어를 호출하고, 대기 필요 시 대기UI로 응답을 교체한다.
    * ==============================================================================================
    */
    public partial class Purchase : Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {
            /* BEGIN OF 유량제어 코드삽입
            ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ */
            // TO-DO : ServiceId/GateId는 데모용으로 동작 확인용. 별도 안내되는 실제 세팅값으로 변경 필요
            string serviceId = "9000"; // 할당받은 SERVICE ID로 수정
            string gateId    = "1";    // 홈(대문)과 동일 GATE ID

            // ServiceId / GateId가 지정된 경우에만 유량제어 동작 (미지정 시 통과)
            if (!string.IsNullOrEmpty(serviceId) && !string.IsNullOrEmpty(gateId))
            {
                try
                {
                    WebGate webgate = new WebGate(serviceId, gateId);

                    // WG_IsNeedToWait() == true → 대기 필요 → 대기UI 표시
                    if (webgate.WG_IsNeedToWait())
                    {
                        Response.Clear();
                        Response.ContentType = "text/html";
                        Response.Write(webgate.WG_GetWaitingUi());
                        Response.Flush();
                        Response.End();
                    }
                }
                catch
                {
                    // 유량제어 호출 실패 시 통과(서비스 중단 방지)
                }

                // 데모 전용 처리 (유량제어 적용 페이지 안내문구 표시용) → 적용하는 페이지에서는 사용할 필요 없음
                var siteMaster = this.Master as SiteMaster;
                if (siteMaster != null)
                {
                    siteMaster.ShowWebGateInfo = true;
                    siteMaster.WebGateServiceId = serviceId;
                }
            }
            /* ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
            END OF 유량제어 코드삽입 */
        }
    }
}
