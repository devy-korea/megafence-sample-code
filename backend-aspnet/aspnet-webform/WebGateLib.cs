using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Web;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;

/*
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for ASP.NET (V26.1)
* ==============================================================================================
* ## 안내 ##
*   본 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
*   1. 본 라이브러리 소스는 프로젝트 Import용으로 별도의 수정 작업은 필요 없습니다.
*   2. .NET Framework 4.5 이상의 환경에서 사용을 권장합니다.
*   3. local pc에서는 정상적으로 동작하지 않을 수 있습니다. SSL(https) 적용된 웹서버에서 테스트를 권장합니다.
* ----------------------------------------------------------------------------------------------
* ## 주의 ##
*   본 라이브러리는 데브와이 등록 특허 실시의 일부분으로 허가된 고객 및 환경 이외의 이용을 금합니다.
*   무단 열람, 복사, 배포, 수정, 실행, 테스트 등의 행위는 권리침해 사유가 될 수 있습니다.
* ----------------------------------------------------------------------------------------------
* written by ysd@devy.co.kr, (c) 2024 DEVY (https://www.devy.kr)
*/

namespace devy.WebGateLib
{
    public class WebGate
    {
        /////////////////////////////////
        // VER 26.1.530
        /////////////////////////////////

        private readonly string     _serviceId;
        private readonly string     _gateId;
        private readonly HttpRequest  REQ;
        private readonly HttpResponse RES;

        public WebGate(string serviceId, string gateId)
        {
            _serviceId = serviceId;
            _gateId    = gateId;
            REQ = HttpContext.Current.Request;
            RES = HttpContext.Current.Response;
        }

        /// <summary>
        /// 대기판정 메인함수 (V26 이후 버전부터는 이 이름 사용 권장)
        /// </summary>
        public bool WG_IsNeedToWait()
        {
            return WG_IsNeedToWaiting();
        }

        /// <summary>
        /// 대기판정 메인함수
        /// true : 대기필요 / false : 대기 불필요
        /// </summary>
        public bool WG_IsNeedToWaiting()
        {
            string       WG_VER_BACKEND         = "26.1.530";
            string       WG_LANG_BACKEND        = "ASPNET/" + Environment.Version.ToString();
            string       WG_SERVICE_ID          = _serviceId;
            string       WG_GATE_ID             = _gateId;
            const int    WG_MAX_TRY_COUNT       = 3;
            bool         WG_IS_CHECKOUT_OK      = false;
            const int    WG_GATE_SERVER_MAX     = 6;
            var          WG_GATE_SERVERS        = new List<string>();
            string       WG_TOKEN_NO            = "";
            string       WG_TOKEN_STATE         = "";
            string       WG_TOKEN_KEY           = "";
            string       WG_WAS_IP              = "";
            string       WG_TRACE               = "WG_IsNeedToWaiting()::";
            int          WG_TRACE_LEVEL         = 0;
            string       WG_IS_LOADTEST         = "N";
            string       WG_REQ_PAGE            = WG_GetRequestPageUrl();
            string       WG_REQ_IP              = REQ.ServerVariables["REMOTE_ADDR"] ?? "";
            string       WG_REFERRER            = WG_GetReferrer();
            string       WG_CLIENT_IP           = WG_GetUserIpAddr();
            int          WG_OUT_COUNT           = 0;
            int          WG_RESULT_CODE         = 0;
            string       WG_RESULT_MESSAGE      = "";
            string       WG_GATE_OPERATION_MODE = "GATE";
            bool         WG_IS_NEED_TO_WAIT     = false;
            bool         WG_RETURN_FLAG         = false;

            // get trace level from cookie
            string traceLevelText = WG_ReadCookie("WG_TRACE_LEVEL");
            if (!string.IsNullOrEmpty(traceLevelText))
            {
                int parsed;
                if (int.TryParse(traceLevelText, out parsed))
                    WG_TRACE_LEVEL = parsed;
            }

            // init was ip list
            for (int i = 0; i < WG_GATE_SERVER_MAX; i++)
                WG_GATE_SERVERS.Add(_serviceId + "-" + i + ".devy.kr");

            // deprecated parameter for load test, will be removed in future version
            if (REQ.QueryString["IsLoadTest"] == "Y")
                WG_IS_LOADTEST = "Y";

            /******************************************************************************
             * STEP-1 : URL Parameter로 대기표 검증 (WG_TOKEN url param이 있으면 처리)
             ******************************************************************************/
            try
            {
                WG_TRACE += "STEP1:";

                string tokenParam = REQ.QueryString["WG_TOKEN"];
                if (!string.IsNullOrEmpty(tokenParam))
                {
                    string[] parameterValues = tokenParam.Split(',');
                    if (parameterValues.Length == 4)
                    {
                        string paramGateId   = parameterValues[0];
                        string paramTokenNo  = parameterValues[1];
                        string paramTokenKey = parameterValues[2];
                        string paramWasIp    = parameterValues[3];

                        if (!string.IsNullOrEmpty(paramTokenNo)  &&
                            !string.IsNullOrEmpty(paramTokenKey) &&
                            !string.IsNullOrEmpty(paramWasIp)    &&
                            paramGateId == WG_GATE_ID)
                        {
                            // API Call by ACTION=OUT
                            if (!WG_RETURN_FLAG)
                            {
                                WG_TRACE += "API_OUT:";

                                string apiUrl = WG_BuildApiUrl(paramWasIp, new Dictionary<string, string> {
                                    { "ServiceId",  WG_SERVICE_ID  },
                                    { "GateId",     WG_GATE_ID     },
                                    { "Action",     "OUT"          },
                                    { "TokenNo",    paramTokenNo   },
                                    { "TokenKey",   paramTokenKey  },
                                    { "ClientIp",   WG_CLIENT_IP   },
                                    { "ReqPage",    WG_REQ_PAGE    },
                                    { "IsLoadTest", WG_IS_LOADTEST }
                                });

                                if (WG_TRACE_LEVEL >= 2) WG_TRACE += apiUrl + ",";

                                string responseText = WG_CallApi(apiUrl, 10);
                                Dictionary<string, object> json = WG_ParseJson(responseText);

                                WG_RESULT_CODE    = WG_GetIntValue(json, "ResultCode", 0);
                                WG_RESULT_MESSAGE = WG_GetStrValue(json, "ResultMessage");

                                WG_TRACE += WG_RESULT_CODE + "/" + WG_GATE_OPERATION_MODE;
                                if (WG_TRACE_LEVEL >= 2) WG_TRACE += ":" + WG_RESULT_MESSAGE + ",";
                                WG_TRACE += ",";

                                // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                                if (WG_RESULT_CODE == 0)
                                {
                                    WG_WAS_IP              = paramWasIp;
                                    WG_TOKEN_KEY           = paramTokenKey;
                                    WG_TOKEN_STATE         = WG_GetStrValue(json, "TokenState");
                                    WG_GATE_OPERATION_MODE = WG_GetStrValue(json, "GateOperationMode", "GATE");
                                    WG_TOKEN_NO            = WG_GetStrValue(json, "TokenNo");
                                    WG_OUT_COUNT           = WG_GetIntValue(json, "OutCount", 0);

                                    if (WG_GATE_OPERATION_MODE == "ALERT")
                                    {
                                        WG_TRACE          += "ALERT,";
                                        WG_RETURN_FLAG     = true;
                                        WG_IS_CHECKOUT_OK  = false;
                                        WG_IS_NEED_TO_WAIT = true;
                                    }
                                    else if (WG_GATE_OPERATION_MODE == "GATE")
                                    {
                                        if (WG_TOKEN_STATE == "WAIT")
                                        {
                                            WG_TRACE          += "WAIT,";
                                            WG_RETURN_FLAG     = true;
                                            WG_IS_CHECKOUT_OK  = false;
                                            WG_IS_NEED_TO_WAIT = true;
                                        }
                                        else if (WG_TOKEN_STATE == "CONNECT" ||
                                                 WG_TOKEN_STATE == "IN"      ||
                                                 WG_TOKEN_STATE == "OUT")
                                        {
                                            WG_TRACE          += "PASS,";
                                            WG_RETURN_FLAG     = true;
                                            WG_IS_CHECKOUT_OK  = true;
                                            WG_IS_NEED_TO_WAIT = false;
                                        }
                                        else
                                        {
                                            WG_TRACE += "FAIL1[TokenState:" + WG_TOKEN_STATE + "],";
                                        }
                                    }
                                    else
                                    {
                                        WG_TRACE += "FAIL2[GateOperationMode:" + WG_GATE_OPERATION_MODE + "],";
                                    }
                                }
                                else
                                {
                                    WG_TRACE += "FAIL3[ResultCode:" + WG_RESULT_CODE + "],";
                                }
                            }
                        }
                        else { WG_TRACE += "SKIP1,"; }
                    }
                    else { WG_TRACE += "SKIP2,"; }
                }
                else { WG_TRACE += "SKIP3,"; }
            }
            catch (Exception ex)
            {
                WG_TRACE += "ERROR:" + ex.Message + ",";
            }

            /******************************************************************************
             * STEP-2 : Cookie로 대기표 검증
             ******************************************************************************/
            try
            {
                WG_TRACE += "→STEP2:";

                if (!WG_RETURN_FLAG)
                {
                    string cookieTokenNo  = WG_ReadCookie("WG_TOKEN_NO");
                    string cookieTokenKey = WG_ReadCookie("WG_CLIENT_ID");
                    string cookieWasIp    = WG_ReadCookie("WG_WAS_IP");

                    if (WG_TRACE_LEVEL >= 1)
                    {
                        WG_TRACE += "WG_TOKEN_NO:"  + (cookieTokenNo  != "" ? cookieTokenNo  : "NULL") + "|"
                                  + "WG_TOKEN_KEY:" + (cookieTokenKey != "" ? cookieTokenKey : "NULL") + "|"
                                  + "WG_WAS_IP:"    + (cookieWasIp    != "" ? cookieWasIp    : "NULL") + ",";
                    }

                    // WG_TOKEN_NO, WG_CLIENT_ID, WG_WAS_IP 쿠키가 모두 있을 때 API Call 시도
                    if (!string.IsNullOrEmpty(cookieTokenNo)  &&
                        !string.IsNullOrEmpty(cookieTokenKey) &&
                        !string.IsNullOrEmpty(cookieWasIp))
                    {
                        // API ACTION=OUT
                        if (!WG_IS_CHECKOUT_OK && !WG_IS_NEED_TO_WAIT)
                        {
                            WG_TRACE += "API_OUT:";

                            string apiUrl = WG_BuildApiUrl(cookieWasIp, new Dictionary<string, string> {
                                { "ServiceId",  WG_SERVICE_ID  },
                                { "GateId",     WG_GATE_ID     },
                                { "Action",     "OUT"          },
                                { "TokenNo",    cookieTokenNo  },
                                { "TokenKey",   cookieTokenKey },
                                { "ClientIp",   WG_CLIENT_IP   },
                                { "ReqPage",    WG_REQ_PAGE    },
                                { "IsLoadTest", WG_IS_LOADTEST }
                            });

                            if (WG_TRACE_LEVEL >= 2) WG_TRACE += apiUrl + ",";

                            string responseText = WG_CallApi(apiUrl, 10);
                            Dictionary<string, object> json = WG_ParseJson(responseText);

                            WG_RESULT_CODE    = WG_GetIntValue(json, "ResultCode", 0);
                            WG_RESULT_MESSAGE = WG_GetStrValue(json, "ResultMessage");

                            WG_TRACE += WG_RESULT_CODE + "/" + WG_GATE_OPERATION_MODE;
                            if (WG_TRACE_LEVEL >= 2) WG_TRACE += ":" + WG_RESULT_MESSAGE + ",";
                            WG_TRACE += ",";

                            // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                            if (WG_RESULT_CODE == 0)
                            {
                                WG_WAS_IP              = cookieWasIp;
                                WG_TOKEN_KEY           = cookieTokenKey;
                                WG_TOKEN_STATE         = WG_GetStrValue(json, "TokenState");
                                WG_GATE_OPERATION_MODE = WG_GetStrValue(json, "GateOperationMode", "GATE");
                                WG_TOKEN_NO            = WG_GetStrValue(json, "TokenNo");
                                WG_OUT_COUNT           = WG_GetIntValue(json, "OutCount", 0);

                                if (WG_GATE_OPERATION_MODE == "ALERT")
                                {
                                    WG_TRACE          += "ALERT,";
                                    WG_RETURN_FLAG     = true;
                                    WG_IS_CHECKOUT_OK  = false;
                                    WG_IS_NEED_TO_WAIT = true;
                                }
                                else if (WG_GATE_OPERATION_MODE == "GATE")
                                {
                                    if (WG_TOKEN_STATE == "WAIT")
                                    {
                                        WG_TRACE          += "WAIT,";
                                        WG_RETURN_FLAG     = true;
                                        WG_IS_CHECKOUT_OK  = false;
                                        WG_IS_NEED_TO_WAIT = true;
                                    }
                                    else if (WG_TOKEN_STATE == "CONNECT" ||
                                             WG_TOKEN_STATE == "IN"      ||
                                             WG_TOKEN_STATE == "OUT")
                                    {
                                        WG_TRACE          += "PASS,";
                                        WG_RETURN_FLAG     = true;
                                        WG_IS_CHECKOUT_OK  = true;
                                        WG_IS_NEED_TO_WAIT = false;
                                    }
                                    else
                                    {
                                        WG_TRACE += "FAIL1[TokenState:" + WG_TOKEN_STATE + "],";
                                    }
                                }
                                else
                                {
                                    WG_TRACE += "FAIL2[GateOperationMode:" + WG_GATE_OPERATION_MODE + "],";
                                }
                            }
                            else
                            {
                                WG_TRACE += "FAIL3[ResultCode:" + WG_RESULT_CODE + "],";
                            }
                        }
                    }
                    else { WG_TRACE += "SKIP1,"; }
                }
                else { WG_TRACE += "SKIP2,"; }
            }
            catch (Exception ex)
            {
                WG_TRACE += "ERROR:" + ex.Message + ",";
            }

            /******************************************************************************
             * STEP-3 : 신규접속자로 간주하고 대기열 표시여부 판단
             ******************************************************************************/
            WG_TRACE += "→STEP3:";
            int tryCount = 0;

            if (!WG_RETURN_FLAG)
            {
                WG_RESULT_CODE = -1;

                int serverCount = WG_GATE_SERVERS.Count;
                int drawResult  = new Random().Next(serverCount);

                // TokenKey 쿠키(WG_CLIENT_ID)가 없으면 신규 발급(채번)
                WG_TOKEN_KEY = WG_ReadCookie("WG_CLIENT_ID");
                if (string.IsNullOrEmpty(WG_TOKEN_KEY))
                {
                    WG_TOKEN_KEY = WG_GetRandomString(12);
                    if (WG_TRACE_LEVEL >= 1) WG_TRACE += "Generate TOKEN:" + WG_TOKEN_KEY + ",";
                    WG_WriteCookie("WG_CLIENT_ID", WG_TOKEN_KEY);
                }

                // 최대 3회 Api call (action=MATCHING) 시도
                for (tryCount = 0; tryCount < WG_MAX_TRY_COUNT; tryCount++)
                {
                    try
                    {
                        WG_TRACE += "API_MATCHING" + (tryCount + 1) + ":";

                        string serverIp;
                        if (tryCount == 0 && !string.IsNullOrEmpty(WG_WAS_IP))
                        {
                            serverIp = WG_WAS_IP;
                        }
                        else
                        {
                            serverIp = WG_WAS_IP = WG_GATE_SERVERS[(drawResult++) % serverCount];
                        }

                        string apiUrl = WG_BuildApiUrl(serverIp, new Dictionary<string, string> {
                            { "ServiceId",  WG_SERVICE_ID  },
                            { "GateId",     WG_GATE_ID     },
                            { "Action",     "MATCHING"     },
                            { "TokenNo",    ""             },
                            { "TokenKey",   WG_TOKEN_KEY   },
                            { "ClientIp",   WG_CLIENT_IP   },
                            { "ReqPage",    WG_REQ_PAGE    },
                            { "IsLoadTest", WG_IS_LOADTEST }
                        });

                        string responseText = WG_CallApi(apiUrl, 3 * (tryCount + 1));
                        Dictionary<string, object> json = WG_ParseJson(responseText);

                        WG_RESULT_CODE    = WG_GetIntValue(json, "ResultCode", 0);
                        WG_RESULT_MESSAGE = WG_GetStrValue(json, "ResultMessage");

                        WG_TRACE += ":" + WG_RESULT_CODE + "/" + WG_GATE_OPERATION_MODE + ",";
                        if (WG_TRACE_LEVEL >= 2) WG_TRACE += WG_RESULT_MESSAGE + ",";
                        WG_TRACE += ",";

                        // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                        if (WG_RESULT_CODE == 0)
                        {
                            WG_TOKEN_STATE         = WG_GetStrValue(json, "TokenState");
                            WG_GATE_OPERATION_MODE = WG_GetStrValue(json, "GateOperationMode", "GATE");
                            WG_TOKEN_NO            = WG_GetStrValue(json, "TokenNo");
                            WG_OUT_COUNT           = WG_GetIntValue(json, "OutCount", 0);

                            if (WG_GATE_OPERATION_MODE == "ALERT")
                            {
                                WG_TRACE          += "ALERT,";
                                WG_RETURN_FLAG     = true;
                                WG_IS_CHECKOUT_OK  = false;
                                WG_IS_NEED_TO_WAIT = true;
                                break;
                            }
                            else if (WG_GATE_OPERATION_MODE == "GATE")
                            {
                                if (WG_TOKEN_STATE == "WAIT")
                                {
                                    WG_TRACE          += "WAIT,";
                                    WG_RETURN_FLAG     = true;
                                    WG_IS_CHECKOUT_OK  = false;
                                    WG_IS_NEED_TO_WAIT = true;
                                    break;
                                }
                                else if (WG_TOKEN_STATE == "CONNECT" ||
                                         WG_TOKEN_STATE == "IN"      ||
                                         WG_TOKEN_STATE == "OUT")
                                {
                                    WG_TRACE          += "PASS,";
                                    WG_RETURN_FLAG     = true;
                                    WG_IS_CHECKOUT_OK  = true;
                                    WG_IS_NEED_TO_WAIT = false;
                                    break;
                                }
                                else
                                {
                                    WG_TRACE += "FAIL1[TokenState:" + WG_TOKEN_STATE + "],";
                                    break;
                                }
                            }
                            else
                            {
                                WG_TRACE += "FAIL2[GateOperationMode:" + WG_GATE_OPERATION_MODE + "],";
                            }
                        }
                        else
                        {
                            WG_TRACE += "FAIL3[ResultCode:" + WG_RESULT_CODE + "],";
                        }
                    }
                    catch (Exception ex)
                    {
                        WG_TRACE += "ERROR:" + ex.Message + ",";
                    }
                }

                if (tryCount >= WG_MAX_TRY_COUNT)
                {
                    WG_TRACE += "RETRY_EXCEEDED,";
                    WG_IS_NEED_TO_WAIT = WG_RESULT_CODE < 0 ? false : true;
                }
            }
            else
            {
                WG_TRACE += "SKIP,";
            }

            WG_TRACE += "TryCount:" + tryCount + ",";

            bool result = WG_IS_NEED_TO_WAIT;

            WG_TRACE += "→returns:" + (result ? "true" : "false") + "," + WG_RESULT_CODE + "," + WG_RESULT_MESSAGE;
            WG_TRACE = WG_LimitTrace(WG_TRACE);

            /* 항상 생성하는 쿠키 */
            WG_WriteCookie("WG_TIME",     DateTime.Now.ToString("o"));
            WG_WriteCookie("WG_GATE_ID",  WG_GATE_ID);
            WG_WriteCookie("WG_WAS_IP",   WG_WAS_IP);
            WG_WriteCookie("WG_TOKEN_NO", WG_TOKEN_NO);
            WG_WriteCookie("WG_OUT_COUNT", WG_OUT_COUNT.ToString());

            /* 조건부 삭제 쿠키 : 존재하는 쿠키만 삭제 (무조건 삭제하지 않음) */
            WG_DeleteCookie("WG_TRACE");
            WG_DeleteCookie("WG_LANG_BACKEND");
            WG_DeleteCookie("WG_VER_BACKEND");
            WG_DeleteCookie("WG_REQ_PAGE");
            WG_DeleteCookie("WG_REQ_IP");
            WG_DeleteCookie("WG_REFERRER");
            WG_DeleteCookie("WG_CLIENT_IP");
            WG_DeleteCookie("WG_TOKEN_STATE");
            WG_DeleteCookie("WG_RESULT_CODE");
            WG_DeleteCookie("WG_GATE_OPERATION_MODE");

            if (WG_TRACE_LEVEL >= 2)
            {
                WG_WriteCookie("WG_TRACE",               WG_TRACE);
                WG_WriteCookie("WG_REQ_PAGE",            WG_REQ_PAGE);
                WG_WriteCookie("WG_REQ_IP",              WG_REQ_IP);
                WG_WriteCookie("WG_GATE_OPERATION_MODE", WG_GATE_OPERATION_MODE);
                WG_WriteCookie("WG_REFERRER",            WG_REFERRER);
                WG_WriteCookie("WG_CLIENT_IP",           WG_CLIENT_IP);
                WG_WriteCookie("WG_TOKEN_STATE",         WG_TOKEN_STATE);
                WG_WriteCookie("WG_RESULT_CODE",         WG_RESULT_CODE.ToString());
            }

            if (WG_TRACE_LEVEL >= 1)
            {
                WG_WriteCookie("WG_LANG_BACKEND", WG_LANG_BACKEND);
                WG_WriteCookie("WG_VER_BACKEND",  WG_VER_BACKEND);
                WG_WriteCookie("WG_TRACE",        WG_TRACE);
            }

            if (WG_TRACE_LEVEL <= 0)
            {
                WG_DeleteCookie("WG_TRACE_LEVEL");
            }

            return result;
        }

        /// <summary>
        /// 호환성 유지. WG_IsNeedToWaiting()만 사용
        /// </summary>
        public bool WG_IsNeedToWaiting_V2()
        {
            return WG_IsNeedToWaiting();
        }

        /// <summary>
        /// 호환성 유지. WG_IsNeedToWaiting()만 사용
        /// </summary>
        public bool WG_IsValidToken()
        {
            return !WG_IsNeedToWaiting();
        }

        /// <summary>
        /// 대기UI HTML return
        /// </summary>
        public string WG_GetWaitingUi()
        {
            string versionTag = DateTime.Now.ToString("yyyyMMddHH00");

            var sb = new StringBuilder();
            sb.AppendLine("<!DOCTYPE html>");
            sb.AppendLine("<html>");
            sb.AppendLine("<head>");
            sb.AppendLine("    <meta http-equiv='X-UA-Compatible' content='IE=edge' />");
            sb.AppendLine("    <meta charset='utf-8' />");
            sb.AppendLine("    <meta http-equiv='cache-control' content='no-cache' />");
            sb.AppendLine("    <meta http-equiv='Expires' content='-1' />");
            sb.AppendLine("    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no' />");
            sb.AppendLine("    <meta name='robots' content='noindex,nofollow'>");
            sb.AppendLine("    <title></title>");
            sb.AppendLine("</head>");
            sb.AppendLine("<body>");
            sb.AppendLine($"    <link href='https://cdn2.devy.kr/{_serviceId}/css/webgate.css?v={versionTag}' rel='stylesheet' />");
            sb.AppendLine($"    <script type='text/javascript' src='https://cdn2.devy.kr/{_serviceId}/js/webgate.js?v={versionTag}'></script>");
            sb.AppendLine("    <script>");
            sb.AppendLine("        function WG_PageLoaded() {");
            sb.AppendLine($"            var gateId = '{_gateId}';");
            sb.AppendLine("            var nextUrl = window.location.href;");
            sb.AppendLine("            var config = {");
            sb.AppendLine("                gateId: gateId,");
            sb.AppendLine("                uiMode: 'BACKEND',");
            sb.AppendLine("                topTitle: '서비스 접속 대기 중',");
            sb.AppendLine("                uiShowDelay: 0 * 1000,");
            sb.AppendLine("                uiHideDelay: 10 * 1000,");
            sb.AppendLine("                resetForce: false,");
            sb.AppendLine("                customType: '',");
            sb.AppendLine("                onSuccess: function(data) {");
            sb.AppendLine("                    var res = $WG.lastResponse;");
            sb.AppendLine("                    if (res.GateOperationMode == 'ALERT') {");
            sb.AppendLine("                        if (res.GateOperationMessageTitle == 'NOT_OPEN') {");
            sb.AppendLine("                            alert(res.GateOperationMessageDetail);");
            sb.AppendLine("                        }");
            sb.AppendLine("                    } else if (res.GateOperationMode == 'GATE') {");
            sb.AppendLine("                        WG_ChangeUrl(nextUrl);");
            sb.AppendLine("                    }");
            sb.AppendLine("                },");
            sb.AppendLine("                onMdsDetected: function(data) {");
            sb.AppendLine("                    if (typeof turnstile != 'undefined' && turnstile != null) {");
            sb.AppendLine("                        turnstile.reset();");
            sb.AppendLine("                    }");
            sb.AppendLine("                    var msg = '매크로 등에 의한 부정접속 시도가 감지되었습니다.\\n만일 \\'사람인지 확인하십시오\\' 문구가 화면에 표시되었다면 확인란을 체크하신 후 다시 시도해 보시기 바랍니다.';");
            sb.AppendLine("                    if (data.ResultMessage == 'MDS_FAIL_BLOCKED_IP') {");
            sb.AppendLine("                        msg = '접속 차단된 IP입니다.\\n다른 IP에서 접속하거나 관리자에게 문의 주시기 바랍니다.';");
            sb.AppendLine("                    }");
            sb.AppendLine("                    alert(msg);");
            sb.AppendLine("                    return;");
            sb.AppendLine("                },");
            sb.AppendLine("                onAlert: function(data) {");
            sb.AppendLine("                    var res = $WG.lastResponse;");
            sb.AppendLine("                    alert(res.GateOperationMessageDetail);");
            sb.AppendLine("                },");
            sb.AppendLine("                onWaiting: function(data) {");
            sb.AppendLine("                },");
            sb.AppendLine("                onFail: function(data) {");
            sb.AppendLine("                    alert('죄송합니다. 잠시 후 다시 시도해 주세요');");
            sb.AppendLine("                },");
            sb.AppendLine("                onFinally: function() {");
            sb.AppendLine("                }");
            sb.AppendLine("            };");
            sb.AppendLine("            WG_StartWebGate(config);");
            sb.AppendLine("        }");
            sb.AppendLine("    </script>");
            sb.AppendLine("</body>");
            sb.AppendLine("</html>");

            return sb.ToString();
        }

        #region Private Helpers

        /// <summary>
        /// Random key 생성 (WG_CLIENT_ID용) - 체크코드 포함
        /// </summary>
        private string WG_GetRandomString(int length)
        {
            string characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            int charLen = characters.Length;

            if (length < 12) length = 12;

            var rand = new Random();
            var sb = new StringBuilder();

            // 본문 생성 (뒤 2자리는 체크코드)
            for (int i = 0; i < (length - 2); i++)
                sb.Append(characters[rand.Next(charLen)]);

            string s = sb.ToString();

            // 최소 문자
            char minChar = s[0];
            for (int i = 1; i < s.Length; i++)
                if (s[i] < minChar) minChar = s[i];
            int minPos = characters.IndexOf(minChar);

            // 최대 문자
            char maxChar = s[0];
            for (int i = 1; i < s.Length; i++)
                if (s[i] > maxChar) maxChar = s[i];
            int maxPos = characters.IndexOf(maxChar);

            // 체크코드 2자리
            char minCheckCode = characters[(minPos - 1 + charLen) % charLen];
            char maxCheckCode = characters[(maxPos + 1 + charLen) % charLen];

            return s + minCheckCode + maxCheckCode;
        }

        /// <summary>
        /// Read Cookie : URL Decoding 지원
        /// </summary>
        private string WG_ReadCookie(string key)
        {
            HttpCookie cookie = REQ.Cookies[key];
            if (cookie != null)
            {
                try { return HttpUtility.UrlDecode(cookie.Value) ?? ""; }
                catch { return cookie.Value ?? ""; }
            }
            return "";
        }

        /// <summary>
        /// Write Cookie : URL Encoding 지원, Lax default
        /// WG_CLIENT_ID는 365일, 나머지는 1일
        /// </summary>
        private void WG_WriteCookie(string key, string value)
        {
            int days = key == "WG_CLIENT_ID" ? 365 : 1;
            string safeValue = HttpUtility.UrlEncode(value ?? "");

            HttpCookie cookie = new HttpCookie(key);
            cookie.Value    = safeValue;
            cookie.Path     = "/";
            cookie.HttpOnly = false;
            cookie.Expires  = DateTime.Now.AddDays(days);
            //cookie.Secure = true; /* local test 지원 위해 주석 처리 */
            try { cookie.SameSite = SameSiteMode.Lax; } catch { }
            RES.Cookies.Set(cookie);
        }

        /// <summary>
        /// Delete cookie : 존재하는 쿠키만 삭제 (요청에 없는 쿠키는 무조건 삭제하지 않음)
        /// </summary>
        private void WG_DeleteCookie(string key)
        {
            // 존재하지 않는 쿠키는 삭제 헤더를 보내지 않음
            if (REQ.Cookies[key] == null)
            {
                return;
            }

            HttpCookie cookie = new HttpCookie(key);
            cookie.Value   = "";
            cookie.Path    = "/";
            cookie.Expires = DateTime.Now.AddYears(-1);
            try { cookie.SameSite = SameSiteMode.Lax; } catch { }
            RES.Cookies.Set(cookie);
        }

        /// <summary>
        /// API URL 생성 (URL 인코딩 적용)
        /// </summary>
        private string WG_BuildApiUrl(string serverIp, Dictionary<string, string> parameters)
        {
            var sb = new StringBuilder();
            sb.Append("https://").Append(serverIp).Append("/?");
            bool first = true;
            foreach (var kv in parameters)
            {
                if (!first) sb.Append("&");
                sb.Append(Uri.EscapeDataString(kv.Key))
                  .Append("=")
                  .Append(Uri.EscapeDataString(kv.Value ?? ""));
                first = false;
            }
            return sb.ToString();
        }

        /// <summary>
        /// JSON 문자열을 Dictionary로 파싱
        /// </summary>
        private Dictionary<string, object> WG_ParseJson(string responseText)
        {
            if (string.IsNullOrEmpty(responseText)) return null;
            try
            {
                var serializer = new JavaScriptSerializer();
                return serializer.Deserialize<Dictionary<string, object>>(responseText);
            }
            catch { return null; }
        }

        private int WG_GetIntValue(Dictionary<string, object> json, string key, int defaultValue)
        {
            if (json == null || !json.ContainsKey(key)) return defaultValue;
            try { return Convert.ToInt32(json[key]); }
            catch { return defaultValue; }
        }

        private string WG_GetStrValue(Dictionary<string, object> json, string key, string defaultValue = "")
        {
            if (json == null || !json.ContainsKey(key)) return defaultValue;
            return json[key]?.ToString() ?? defaultValue;
        }

        /// <summary>
        /// API call 후 응답 문자열 반환
        /// 오류 시 ErrorJson 문자열 반환
        /// </summary>
        private string WG_CallApi(string url, int timeoutSeconds)
        {
            if (!WG_IsValidApiUrl(url))
                return WG_MakeErrorJson(-1001, "Invalid API URL");

            int timeout = timeoutSeconds <= 0 ? 10 : timeoutSeconds;

            try
            {
                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
                request.Method          = "GET";
                request.Timeout         = timeout * 1000;
                request.ReadWriteTimeout = timeout * 1000;
                request.UserAgent       = "WebGate-ASPNET";
                request.Accept          = "application/json,text/plain,*/*";
                request.AllowAutoRedirect = false;
                // SSL 인증서 검증 생략 (내부망 API 서버 전용)
                ServicePointManager.ServerCertificateValidationCallback =
                    (sender, certificate, chain, sslPolicyErrors) => true;

                try
                {
                    using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                    {
                        int httpCode = (int)response.StatusCode;
                        using (StreamReader sr = new StreamReader(response.GetResponseStream(), Encoding.UTF8))
                        {
                            string responseText = sr.ReadToEnd();
                            if (httpCode < 200 || httpCode >= 300)
                                return WG_MakeErrorJson(-1200 - httpCode, "HTTP Error: " + httpCode);
                            return responseText;
                        }
                    }
                }
                catch (WebException wex) when (wex.Response != null)
                {
                    int httpCode = (int)((HttpWebResponse)wex.Response).StatusCode;
                    return WG_MakeErrorJson(-1200 - httpCode, "HTTP Error: " + httpCode);
                }
            }
            catch (WebException ex) when (ex.Status == WebExceptionStatus.Timeout)
            {
                return WG_MakeErrorJson(-1101, "Timeout: " + ex.Message);
            }
            catch (Exception ex)
            {
                return WG_MakeErrorJson(-1999, "Exception: " + ex.Message);
            }
        }

        private string WG_MakeErrorJson(int resultCode, string resultMessage)
        {
            string safe = (resultMessage ?? "")
                .Replace("\\", "\\\\")
                .Replace("\"", "\\\"")
                .Replace("\r", " ")
                .Replace("\n", " ");
            return "{\"ResultCode\":" + resultCode + ",\"ResultMessage\":\"" + safe + "\"}";
        }

        private string WG_LimitTrace(string trace)
        {
            if (trace == null) return "";
            return trace.Length > 1000 ? trace.Substring(0, 1000) : trace;
        }

        /// <summary>
        /// SSRF 대응용 API URL 검증
        /// </summary>
        private static bool WG_IsValidApiUrl(string url)
        {
            if (string.IsNullOrEmpty(url)) return false;
            Regex regEx = new Regex(
                @"^http[s]?://\d{4,20}-\w{1,10}\.devy\.kr[/\?].*$",
                RegexOptions.IgnoreCase | RegexOptions.Compiled
            );
            return regEx.IsMatch(url);
        }

        /// <summary>
        /// 사용자(Browser) IP 주소를 X-Forwarded-For 헤더 기반으로 구하는 함수
        /// </summary>
        private string WG_GetUserIpAddr()
        {
            string remoteAddr = REQ.ServerVariables["REMOTE_ADDR"] ?? "";
            string xff        = REQ.ServerVariables["HTTP_X_FORWARDED_FOR"] ?? "";

            if (!string.IsNullOrEmpty(xff))
            {
                string ip = xff.Split(',')[0].Trim();
                System.Net.IPAddress ipAddr;
                if (System.Net.IPAddress.TryParse(ip, out ipAddr))
                    return ip;
            }

            return remoteAddr;
        }

        /// <summary>
        /// 요청페이지 URL을 100자까지 반환하는 함수
        /// </summary>
        private string WG_GetRequestPageUrl()
        {
            string scheme = REQ.ServerVariables["HTTP_X_FORWARDED_PROTO"] ?? "";
            if (string.IsNullOrEmpty(scheme))
                scheme = REQ.IsSecureConnection ? "https" : "http";

            string host    = REQ.ServerVariables["HTTP_HOST"] ?? "";
            string rawUrl  = REQ.RawUrl ?? "";
            string fullUrl = scheme + "://" + host + rawUrl;

            return fullUrl.Length > 100 ? fullUrl.Substring(0, 100) : fullUrl;
        }

        /// <summary>
        /// Referrer URL을 100자까지 반환하는 함수
        /// </summary>
        private string WG_GetReferrer()
        {
            string referrer = REQ.ServerVariables["HTTP_REFERER"] ?? "";
            return referrer.Length > 100 ? referrer.Substring(0, 100) : referrer;
        }

        #endregion
    }
}
