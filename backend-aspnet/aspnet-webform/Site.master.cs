using System;
using System.Web.UI;

namespace AspNetWebForm
{
    // ToTheMoon 마이크로사이트 공통 마스터 페이지 (상단 네비/스타일/푸터 공유)
    public partial class SiteMaster : MasterPage
    {
        // 유량제어 적용 페이지(홈/구매)에서 true로 설정 → 개발자 정보 Fixed 배너 표시
        public bool ShowWebGateInfo { get; set; } = false;

        // 배너에 표시할 ServiceId (페이지에서 설정, 기본 9000)
        public string WebGateServiceId { get; set; } = "9000";

        protected void Page_Load(object sender, EventArgs e)
        {
        }
    }
}
