<?php
/* 
* ==============================================================================================
* 메가펜스 유량제어서비스 Backend Library for PHP (V26.1)
* ==============================================================================================
* ## 안내 ##
*   본 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
*   1. 본 라이브러리 소스는 프로젝트 Import용으로 별도의 수정 작업은 필요 없습니다.
*   2. PHP 8.0 이상의 환경에서 사용을 권장합니다.
*   3. local pc에서는 정상적으로 동작하지 않을 수 있습니다. SSL(https) 적용된 웹서버에서 테스트를 권장합니다.  
* ----------------------------------------------------------------------------------------------
* ## 주의 ##
*   본 라이브러리는 데브와이 등록 특허 실시의 일부분으로 허가된 고객 및 환경 이외의 이용을 금합니다.
*   무단 열람, 복사, 배포, 수정, 실행, 테스트 등의 행위는 권리침해 사유가 될 수 있습니다.
* ----------------------------------------------------------------------------------------------
* written by ysd@devy.co.kr, (c) 2024 DEVY (https://www.devy.kr)
*/

/**
 * 대기판정 메인함수 (V26 이후 버전부터는 이 이름 사용 권장)
 */
function WG_IsNeedToWait($service_id, $gate_id)
{
    return WG_IsNeedToWaiting($service_id, $gate_id);
}

/**
 * 대기판정 메인함수
 * return value
 *   true  : 대기필요
 *   false : 대기 불필요
 */
function WG_IsNeedToWaiting($service_id, $gate_id)
{
    $WG_VER_BACKEND         = "26.1.530";
    $WG_LANG_BACKEND        = "PHP/" . PHP_VERSION;
    $WG_SERVICE_ID          = $service_id;
    $WG_GATE_ID             = $gate_id;
    $WG_MAX_TRY_COUNT       = 3;
    $WG_IS_CHECKOUT_OK      = false;
    $WG_GATE_SERVER_MAX     = 6;
    $WG_GATE_SERVERS        = array();
    $WG_TOKEN_NO            = "";
    $WG_TOKEN_STATE         = "";
    $WG_TOKEN_KEY           = "";
    $WG_WAS_IP              = "";
    $WG_TRACE               = "WG_IsNeedToWaiting()::";
    $WG_TRACE_LEVEL         = 0;
    $WG_IS_LOADTEST         = "N";
    $WG_REQ_PAGE            = WG_GetRequstPageUrl();
    $WG_REQ_IP              = isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : "";
    $WG_REFERRER            = WG_GetReferrer();
    $WG_CLIENT_IP           = WG_GetUserIpAddr();
    $WG_OUT_COUNT           = 0;
    $WG_RESULT_CODE         = 0;
    $WG_RESULT_MESSAGE      = "";
    $WG_GATE_OPERATION_MODE = "GATE";
    $WG_IS_NEED_TO_WAIT     = false;
    $WG_RETURN_FLAG         = false;

    $traceLevelText = WG_ReadCookie("WG_TRACE_LEVEL");
    if ($traceLevelText !== null && $traceLevelText !== '') {
        $WG_TRACE_LEVEL = intval($traceLevelText);
    }

    for ($i = 0; $i < $WG_GATE_SERVER_MAX; $i++) {
        array_push($WG_GATE_SERVERS, $service_id . "-" . $i . ".devy.kr");
    }

    if (isset($_GET["IsLoadTest"]) && $_GET["IsLoadTest"] == "Y") {
        $WG_IS_LOADTEST = "Y";
    }

    /******************************************************************************
    STEP-1 : URL Parameter로 대기표 검증 (WG_TOKEN url param이 있으면 처리)
    *******************************************************************************/
    try {

        $WG_TRACE .= "STEP1:";

        if (isset($_GET["WG_TOKEN"])) {

            $parameterValues = explode(",", $_GET["WG_TOKEN"]);

            if (count($parameterValues) == 4) {

                $paramGateId  = $parameterValues[0];
                $paramTokenNo = $parameterValues[1];
                $paramTokenKey = $parameterValues[2];
                $paramWasIp   = $parameterValues[3];

                if (
                    $paramTokenNo  !== "" &&
                    $paramTokenKey !== "" &&
                    $paramWasIp    !== "" &&
                    strcmp($paramGateId, $WG_GATE_ID) == 0
                ) {

                    // API Call by ACTION=OUT
                    if ($WG_RETURN_FLAG == false) {

                        $WG_TRACE .= "API_OUT:";

                        $apiUrl = WG_BuildApiUrl($paramWasIp, array(
                            "ServiceId"  => $WG_SERVICE_ID,
                            "GateId"     => $WG_GATE_ID,
                            "Action"     => "OUT",
                            "TokenNo"    => $paramTokenNo,
                            "TokenKey"   => $paramTokenKey,
                            "ClientIp"   => $WG_CLIENT_IP,
                            "ReqPage"    => $WG_REQ_PAGE,
                            "IsLoadTest" => $WG_IS_LOADTEST
                        ));

                        if ($WG_TRACE_LEVEL >= 2) {
                            $WG_TRACE .= $apiUrl . ",";
                        }

                        $responseText = WG_CallApi($apiUrl, 10);
                        $json = WG_ParseJson($responseText);

                        $WG_RESULT_CODE    = ($json !== null && isset($json['ResultCode']))    ? intval($json['ResultCode'])    : 0;
                        $WG_RESULT_MESSAGE = ($json !== null && isset($json['ResultMessage'])) ? $json['ResultMessage']         : "";

                        $WG_TRACE .= $WG_RESULT_CODE . "/" . $WG_GATE_OPERATION_MODE;
                        if ($WG_TRACE_LEVEL >= 2) {
                            $WG_TRACE .= ":" . $WG_RESULT_MESSAGE . ",";
                        }
                        $WG_TRACE .= ",";

                        // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                        if ($WG_RESULT_CODE == 0) {

                            $WG_WAS_IP              = $paramWasIp;
                            $WG_TOKEN_KEY           = $paramTokenKey;
                            $WG_TOKEN_STATE         = isset($json['TokenState'])         ? $json['TokenState']         : "";
                            $WG_GATE_OPERATION_MODE = isset($json['GateOperationMode'])  ? $json['GateOperationMode']  : "GATE";
                            $WG_TOKEN_NO            = isset($json['TokenNo'])            ? $json['TokenNo']            : "";
                            $WG_OUT_COUNT           = isset($json['OutCount'])           ? intval($json['OutCount'])   : 0;

                            if ($WG_GATE_OPERATION_MODE == "ALERT") {

                                $WG_TRACE .= "ALERT,";
                                $WG_RETURN_FLAG     = true;
                                $WG_IS_CHECKOUT_OK  = false;
                                $WG_IS_NEED_TO_WAIT = true;

                            } else if ($WG_GATE_OPERATION_MODE == "GATE") {

                                if ($WG_TOKEN_STATE == "WAIT") {

                                    $WG_TRACE .= "WAIT,";
                                    $WG_RETURN_FLAG     = true;
                                    $WG_IS_CHECKOUT_OK  = false;
                                    $WG_IS_NEED_TO_WAIT = true;

                                } else if (
                                    $WG_TOKEN_STATE == "CONNECT" ||
                                    $WG_TOKEN_STATE == "IN" ||
                                    $WG_TOKEN_STATE == "OUT"
                                ) {

                                    $WG_TRACE .= "PASS,";
                                    $WG_RETURN_FLAG     = true;
                                    $WG_IS_CHECKOUT_OK  = true;
                                    $WG_IS_NEED_TO_WAIT = false;

                                } else {

                                    $WG_TRACE .= "FAIL1[TokenState:" . $WG_TOKEN_STATE . "],";
                                }

                            } else {

                                $WG_TRACE .= "FAIL2[GateOperationMode:" . $WG_GATE_OPERATION_MODE . "],";
                            }

                        } else {

                            $WG_TRACE .= "FAIL3[ResultCode:" . $WG_RESULT_CODE . "],";
                        }
                    }

                } else {

                    $WG_TRACE .= "SKIP1,";
                }

            } else {

                $WG_TRACE .= "SKIP2,";
            }

        } else {

            $WG_TRACE .= "SKIP3,";
        }

    } catch (Exception $e) {

        $WG_TRACE .= "ERROR:" . $e->getMessage() . ",";
    }

    /******************************************************************************
    STEP-2 : Cookie로 대기표 검증
    *******************************************************************************/
    try {

        $WG_TRACE .= "→STEP2:";

        if ($WG_RETURN_FLAG == false) {

            $cookieTokenNo  = WG_ReadCookie("WG_TOKEN_NO");
            $cookieTokenKey = WG_ReadCookie("WG_CLIENT_ID");
            $cookieWasIp    = WG_ReadCookie("WG_WAS_IP");

            if ($WG_TRACE_LEVEL >= 1) {
                $WG_TRACE .= "WG_TOKEN_NO:"  . ($cookieTokenNo  !== "" ? $cookieTokenNo  : "NULL") . "|"
                           . "WG_TOKEN_KEY:" . ($cookieTokenKey !== "" ? $cookieTokenKey : "NULL") . "|"
                           . "WG_WAS_IP:"    . ($cookieWasIp    !== "" ? $cookieWasIp    : "NULL") . ",";
            }

            // WG_TOKEN_NO, WG_CLIENT_ID, WG_WAS_IP 쿠키가 모두 있을 때 API Call 시도
            if (
                strlen($cookieTokenNo)  > 0 &&
                strlen($cookieTokenKey) > 0 &&
                strlen($cookieWasIp)    > 0
            ) {

                // API ACTION=OUT
                if ($WG_IS_CHECKOUT_OK == false && $WG_IS_NEED_TO_WAIT == false) {

                    $WG_TRACE .= "API_OUT:";

                    $apiUrl = WG_BuildApiUrl($cookieWasIp, array(
                        "ServiceId"  => $WG_SERVICE_ID,
                        "GateId"     => $WG_GATE_ID,
                        "Action"     => "OUT",
                        "TokenNo"    => $cookieTokenNo,
                        "TokenKey"   => $cookieTokenKey,
                        "ClientIp"   => $WG_CLIENT_IP,
                        "ReqPage"    => $WG_REQ_PAGE,
                        "IsLoadTest" => $WG_IS_LOADTEST
                    ));

                    if ($WG_TRACE_LEVEL >= 2) {
                        $WG_TRACE .= $apiUrl . ",";
                    }

                    $responseText = WG_CallApi($apiUrl, 10);
                    $json = WG_ParseJson($responseText);

                    $WG_RESULT_CODE    = ($json !== null && isset($json['ResultCode']))    ? intval($json['ResultCode'])  : 0;
                    $WG_RESULT_MESSAGE = ($json !== null && isset($json['ResultMessage'])) ? $json['ResultMessage']       : "";

                    $WG_TRACE .= $WG_RESULT_CODE . "/" . $WG_GATE_OPERATION_MODE;
                    if ($WG_TRACE_LEVEL >= 2) {
                        $WG_TRACE .= ":" . $WG_RESULT_MESSAGE . ",";
                    }
                    $WG_TRACE .= ",";

                    // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                    if ($WG_RESULT_CODE == 0) {

                        $WG_WAS_IP              = $cookieWasIp;
                        $WG_TOKEN_KEY           = $cookieTokenKey;
                        $WG_TOKEN_STATE         = isset($json['TokenState'])        ? $json['TokenState']        : "";
                        $WG_GATE_OPERATION_MODE = isset($json['GateOperationMode']) ? $json['GateOperationMode'] : "GATE";
                        $WG_TOKEN_NO            = isset($json['TokenNo'])           ? $json['TokenNo']           : "";
                        $WG_OUT_COUNT           = isset($json['OutCount'])          ? intval($json['OutCount'])  : 0;

                        if ($WG_GATE_OPERATION_MODE == "ALERT") {

                            $WG_TRACE .= "ALERT,";
                            $WG_RETURN_FLAG     = true;
                            $WG_IS_CHECKOUT_OK  = false;
                            $WG_IS_NEED_TO_WAIT = true;

                        } else if ($WG_GATE_OPERATION_MODE == "GATE") {

                            if ($WG_TOKEN_STATE == "WAIT") {

                                $WG_TRACE .= "WAIT,";
                                $WG_RETURN_FLAG     = true;
                                $WG_IS_CHECKOUT_OK  = false;
                                $WG_IS_NEED_TO_WAIT = true;

                            } else if (
                                $WG_TOKEN_STATE == "CONNECT" ||
                                $WG_TOKEN_STATE == "IN" ||
                                $WG_TOKEN_STATE == "OUT"
                            ) {

                                $WG_TRACE .= "PASS,";
                                $WG_RETURN_FLAG     = true;
                                $WG_IS_CHECKOUT_OK  = true;
                                $WG_IS_NEED_TO_WAIT = false;

                            } else {

                                $WG_TRACE .= "FAIL1[TokenState:" . $WG_TOKEN_STATE . "],";
                            }

                        } else {

                            $WG_TRACE .= "FAIL2[GateOperationMode:" . $WG_GATE_OPERATION_MODE . "],";
                        }

                    } else {

                        $WG_TRACE .= "FAIL3[ResultCode:" . $WG_RESULT_CODE . "],";
                    }
                }

            } else {

                $WG_TRACE .= "SKIP1,";
            }

        } else {

            $WG_TRACE .= "SKIP2,";
        }

    } catch (Exception $e) {

        $WG_TRACE .= "ERROR:" . $e->getMessage() . ",";
    }

    /******************************************************************************
    STEP-3 : 신규접속자로 간주하고 대기열 표시여부 판단
    *******************************************************************************/
    $WG_TRACE .= "→STEP3:";
    $tryCount = 0;

    if ($WG_RETURN_FLAG == false) {

        $WG_RESULT_CODE = -1;

        $serverCount = count($WG_GATE_SERVERS);
        $drawResult  = rand(0, $serverCount - 1);

        // TokenKey 쿠키(WG_CLIENT_ID)가 없으면 신규 발급(채번)
        $WG_TOKEN_KEY = WG_ReadCookie("WG_CLIENT_ID");
        if ($WG_TOKEN_KEY == "") {
            $WG_TOKEN_KEY = WG_GetRandomString(12);
            if ($WG_TRACE_LEVEL >= 1) {
                $WG_TRACE .= "Generate TOKEN:" . $WG_TOKEN_KEY . ",";
            }
            WG_WriteCookie("WG_CLIENT_ID", $WG_TOKEN_KEY);
        }

        // 최대 3회 Api call (action=MATCHING) 시도
        for ($tryCount = 0; $tryCount < $WG_MAX_TRY_COUNT; $tryCount++) {

            try {

                $WG_TRACE .= "API_MATCHING" . ($tryCount + 1) . ":";

                if ($tryCount == 0 && strlen($WG_WAS_IP) > 0) {
                    $serverIp = $WG_WAS_IP;
                } else {
                    $serverIp = $WG_WAS_IP =
                        $WG_GATE_SERVERS[($drawResult++) % ($serverCount)];
                }

                $apiUrl = WG_BuildApiUrl($serverIp, array(
                    "ServiceId"  => $WG_SERVICE_ID,
                    "GateId"     => $WG_GATE_ID,
                    "Action"     => "MATCHING",
                    "TokenNo"    => "",
                    "TokenKey"   => $WG_TOKEN_KEY,
                    "ClientIp"   => $WG_CLIENT_IP,
                    "ReqPage"    => $WG_REQ_PAGE,
                    "IsLoadTest" => $WG_IS_LOADTEST
                ));

                $responseText = WG_CallApi($apiUrl, 3 * ($tryCount + 1));
                $json = WG_ParseJson($responseText);

                $WG_RESULT_CODE    = ($json !== null && isset($json['ResultCode']))    ? intval($json['ResultCode']) : 0;
                $WG_RESULT_MESSAGE = ($json !== null && isset($json['ResultMessage'])) ? $json['ResultMessage']      : "";

                $WG_TRACE .= ":" . $WG_RESULT_CODE . "/" . $WG_GATE_OPERATION_MODE . ",";
                if ($WG_TRACE_LEVEL >= 2) {
                    $WG_TRACE .= $WG_RESULT_MESSAGE . ",";
                }
                $WG_TRACE .= ",";

                // 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                if ($WG_RESULT_CODE == 0) {

                    $WG_TOKEN_STATE         = isset($json['TokenState'])        ? $json['TokenState']        : "";
                    $WG_GATE_OPERATION_MODE = isset($json['GateOperationMode']) ? $json['GateOperationMode'] : "GATE";
                    $WG_TOKEN_NO            = isset($json['TokenNo'])           ? $json['TokenNo']           : "";
                    $WG_OUT_COUNT           = isset($json['OutCount'])          ? intval($json['OutCount'])  : 0;

                    if ($WG_GATE_OPERATION_MODE == "ALERT") {

                        $WG_TRACE .= "ALERT,";
                        $WG_RETURN_FLAG     = true;
                        $WG_IS_CHECKOUT_OK  = false;
                        $WG_IS_NEED_TO_WAIT = true;
                        break;

                    } else if ($WG_GATE_OPERATION_MODE == "GATE") {

                        if ($WG_TOKEN_STATE == "WAIT") {

                            $WG_TRACE .= "WAIT,";
                            $WG_RETURN_FLAG     = true;
                            $WG_IS_CHECKOUT_OK  = false;
                            $WG_IS_NEED_TO_WAIT = true;
                            break;

                        } else if (
                            $WG_TOKEN_STATE == "CONNECT" ||
                            $WG_TOKEN_STATE == "IN" ||
                            $WG_TOKEN_STATE == "OUT"
                        ) {

                            $WG_TRACE .= "PASS,";
                            $WG_RETURN_FLAG     = true;
                            $WG_IS_CHECKOUT_OK  = true;
                            $WG_IS_NEED_TO_WAIT = false;
                            break;

                        } else {

                            $WG_TRACE .= "FAIL1[TokenState:" . $WG_TOKEN_STATE . "],";
                            break;
                        }

                    } else {

                        $WG_TRACE .= "FAIL2[GateOperationMode:" . $WG_GATE_OPERATION_MODE . "],";
                    }

                } else {

                    $WG_TRACE .= "FAIL3[ResultCode:" . $WG_RESULT_CODE . "],";
                }

            } catch (Exception $e) {

                $WG_TRACE .= "ERROR:" . $e->getMessage() . ",";
            }
        }

        if ($tryCount >= $WG_MAX_TRY_COUNT) {

            $WG_TRACE .= "RETRY_EXCEEDED,";

            if ($WG_RESULT_CODE < 0) {
                $WG_IS_NEED_TO_WAIT = false;
            } else {
                $WG_IS_NEED_TO_WAIT = true;
            }
        }

    } else {

        $WG_TRACE .= "SKIP,";
    }

    $WG_TRACE .= "TryCount:" . $tryCount . ",";

    $result = $WG_IS_NEED_TO_WAIT;

    $WG_TRACE .= "→returns:" . ($result ? "true" : "false") . "," . $WG_RESULT_CODE . "," . $WG_RESULT_MESSAGE;
    $WG_TRACE = WG_LimitTrace($WG_TRACE);

    /* 항상 생성하는 쿠키 */
    WG_WriteCookie("WG_TIME", date("c"));
    WG_WriteCookie("WG_GATE_ID", $WG_GATE_ID);
    WG_WriteCookie("WG_WAS_IP", $WG_WAS_IP);
    WG_WriteCookie("WG_TOKEN_NO", $WG_TOKEN_NO);
    WG_WriteCookie("WG_OUT_COUNT", $WG_OUT_COUNT);

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

    if ($WG_TRACE_LEVEL >= 2) {
        WG_WriteCookie("WG_TRACE",               $WG_TRACE);
        WG_WriteCookie("WG_REQ_PAGE",            $WG_REQ_PAGE);
        WG_WriteCookie("WG_REQ_IP",              $WG_REQ_IP);
        WG_WriteCookie("WG_GATE_OPERATION_MODE", $WG_GATE_OPERATION_MODE);
        WG_WriteCookie("WG_REFERRER",            $WG_REFERRER);
        WG_WriteCookie("WG_CLIENT_IP",           $WG_CLIENT_IP);
        WG_WriteCookie("WG_TOKEN_STATE",         $WG_TOKEN_STATE);
        WG_WriteCookie("WG_RESULT_CODE",         $WG_RESULT_CODE);
    }

    if ($WG_TRACE_LEVEL >= 1) {
        WG_WriteCookie("WG_LANG_BACKEND", $WG_LANG_BACKEND);
        WG_WriteCookie("WG_VER_BACKEND",  $WG_VER_BACKEND);
        WG_WriteCookie("WG_TRACE",        $WG_TRACE);
    }

    if ($WG_TRACE_LEVEL <= 0) {
        WG_DeleteCookie("WG_TRACE_LEVEL");
    }

    return $result;
}

/**
 * 호환성 유지용 함수
 */
function WG_IsNeedToWaiting_V2($service_id, $gate_id)
{
    return WG_IsNeedToWaiting($service_id, $gate_id);
}

/**
 * 호환성 유지용 함수
 */
function WG_IsValidToken($service_id, $gate_id)
{
    return !WG_IsNeedToWaiting($service_id, $gate_id);
}

/**
 * 대기UI HTML을 반환하는 함수
 */
function WG_GetWaitingUi($service_id, $gate_id)
{
    $versionTag = date("YmdH");
    $host = $_SERVER['HTTP_HOST'] ?? '';
    $devHosts = array('dev.devy.kr');
    $isDevSite = in_array($host, $devHosts);

    $html = "<!DOCTYPE html>\r\n"
        . "<html>\r\n"
        . "<head>\r\n"
        . "    <meta http-equiv='X-UA-Compatible' content='IE=edge'/>\r\n"
        . "    <meta charset='utf-8'/>\r\n"
        . "    <meta http-equiv='cache-control' content='no-cache' />\r\n"
        . "    <meta http-equiv='Expires' content='-1' />\r\n"
        . "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'/>\r\n"
        . "    <meta name='robots' content='noindex,nofollow'>\r\n"
        . "    <title></title>\r\n"
        . "</head>\r\n"
        . "<body>\r\n"
        . "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" . $versionTag . "' rel='stylesheet' />\r\n"
        . "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" . $versionTag . "'></script>\r\n"
        . "    <script>\r\n"
        . "        function WG_PageLoaded() {\r\n"
        . "            var gateId = 'WG_GATE_ID';\r\n"
        . "            var nextUrl = window.location.href;\r\n"
        . "            var config = {\r\n"
        . "                gateId: gateId,\r\n"
        . "                uiMode: 'BACKEND',\r\n"
        . "                topTitle: '서비스 접속 대기 중',\r\n"
        . "                uiShowDelay: 0 * 1000,\r\n"
        . "                uiHideDelay: 10 * 1000,\r\n"
        . "                resetForce: false,\r\n"
        . "                customType: '',\r\n"
        . "                onSuccess: function(data) {\r\n"
        . "                    var res = \$WG.lastResponse;\r\n"
        . "                    if (res.GateOperationMode == 'ALERT') {\r\n"
        . "                        if (res.GateOperationMessageTitle == 'NOT_OPEN') {\r\n"
        . "                            alert(res.GateOperationMessageDetail);\r\n"
        . "                        }\r\n"
        . "                    } else if (res.GateOperationMode == 'GATE') {\r\n"
        . "                        WG_ChangeUrl(nextUrl);\r\n"
        . "                    }\r\n"
        . "                },\r\n"
        . "                onMdsDetected: function(data) {\r\n"
        . "                    if (typeof turnstile != 'undefined' && turnstile != null) {\r\n"
        . "                        turnstile.reset();\r\n"
        . "                    }\r\n"
        . "                    var msg = '매크로 등에 의한 부정접속 시도가 감지되었습니다.\\n만일 \\'사람인지 확인하십시오\\' 문구가 화면에 표시되었다면 확인란을 체크하신 후 다시 시도해 보시기 바랍니다.';\r\n"
        . "                    if (data.ResultMessage == 'MDS_FAIL_BLOCKED_IP') {\r\n"
        . "                        msg = '접속 차단된 IP입니다.\\n다른 IP에서 접속하거나 관리자에게 문의 주시기 바랍니다.';\r\n"
        . "                    }\r\n"
        . "                    alert(msg);\r\n"
        . "                    return;\r\n"
        . "                },\r\n"
        . "                onAlert: function(data) {\r\n"
        . "                    var res = \$WG.lastResponse;\r\n"
        . "                    alert(res.GateOperationMessageDetail);\r\n"
        . "                },\r\n"
        . "                onWaiting: function(data) {\r\n"
        . "                },\r\n"
        . "                onFail: function(data) {\r\n"
        . "                    alert('죄송합니다. 잠시 후 다시 시도해 주세요');\r\n"
        . "                },\r\n"
        . "                onFinally: function() {\r\n"
        . "                }\r\n"
        . "            };\r\n"
        . "            WG_StartWebGate(config);\r\n"
        . "        }\r\n"
        . "    </script>\r\n"
        . "</body>\r\n"
        . "</html>\r\n";

    if ($isDevSite) {
        $html = str_replace("https://cdn2.devy.kr/WG_SERVICE_ID", ".", $html);
    }

    $html = str_replace("WG_SERVICE_ID", $service_id, $html);
    $html = str_replace("WG_GATE_ID", $gate_id, $html);

    return $html;
}

/**
 * 랜덤 문자열 생성 함수 (WG_CLIENT_ID 생성용)
 */
function WG_GetRandomString($length = 12)
{
    $characters = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $charLen = strlen($characters);
    $randstring = '';

    if ($length < 12) {
        $length = 12;
    }

    for ($i = 0; $i < ($length - 2); $i++) {
        $randstring .= $characters[rand(0, $charLen - 1)];
    }

    $minChar = $randstring[0];
    $maxChar = $randstring[0];

    for ($i = 1; $i < strlen($randstring); $i++) {
        if ($randstring[$i] < $minChar) {
            $minChar = $randstring[$i];
        }
        if ($randstring[$i] > $maxChar) {
            $maxChar = $randstring[$i];
        }
    }

    $minPos = strpos($characters, $minChar);
    $maxPos = strpos($characters, $maxChar);

    return $randstring
        . $characters[($minPos - 1 + $charLen) % $charLen]
        . $characters[($maxPos + 1 + $charLen) % $charLen];
}

/**
 * Cookie 읽기 함수
 * 저장 시 rawurlencode 된 값을 rawurldecode 해서 반환
 */
function WG_ReadCookie($key)
{
    if (isset($_COOKIE[$key])) {
        return rawurldecode($_COOKIE[$key]);
    }
    return "";
}

/**
 * Cookie 쓰기 함수
 * WG_CLIENT_ID는 1년, 나머지는 1일로 설정
 */
function WG_WriteCookie($key, $value)
{
    $days = ($key === "WG_CLIENT_ID") ? 365 : 1;
    $safeValue = rawurlencode((string)$value);

    if (PHP_VERSION_ID >= 70300) {
        setcookie($key, $safeValue, array(
            'expires'  => time() + (86400 * $days),
            'path'     => '/',
            'secure'   => false,
            'httponly' => false,
            'samesite' => 'Lax'
        ));
    } else {
        setcookie($key, $safeValue, time() + (86400 * $days), "/; samesite=Lax", "", false, false);
    }
}

/**
 * Cookie 삭제 함수 : 존재하는 쿠키만 삭제 (요청에 없는 쿠키는 무조건 삭제하지 않음)
 */
function WG_DeleteCookie($key)
{
    // 존재하지 않는 쿠키는 삭제 헤더를 보내지 않음
    if (!isset($_COOKIE[$key])) {
        return;
    }

    if (PHP_VERSION_ID >= 70300) {
        setcookie($key, "", array(
            'expires'  => time() - 3600,
            'path'     => '/',
            'secure'   => false,
            'httponly' => false,
            'samesite' => 'Lax'
        ));
    } else {
        setcookie($key, "", time() - 3600, "/; samesite=Lax", "", false, false);
    }
}

/**
 * 사용자(Browser) IP 주소를 X-Forwarded-For 헤더 기반으로 구하는 함수
 */
function WG_GetUserIpAddr()
{
    $remoteAddr = $_SERVER['REMOTE_ADDR'] ?? "";
    $xff = $_SERVER['HTTP_X_FORWARDED_FOR'] ?? "";

    if ($xff !== "") {
        $ipList = explode(',', $xff);
        $ip = trim($ipList[0]);

        if (filter_var($ip, FILTER_VALIDATE_IP)) {
            return $ip;
        }
    }

    return $remoteAddr;
}

/**
 * 요청페이지 URL을 100자까지 안전하게 반환하는 함수
 */
function WG_GetRequstPageUrl()
{
    $scheme = (!empty($_SERVER['HTTP_X_FORWARDED_PROTO']))
        ? $_SERVER['HTTP_X_FORWARDED_PROTO']
        : ((!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http');

    $host = $_SERVER['HTTP_HOST'] ?? '';
    $uri  = $_SERVER['REQUEST_URI'] ?? '';
    $fullUrl = $scheme . '://' . $host . $uri;

    if (function_exists('mb_substr')) {
        return mb_substr($fullUrl, 0, 100, 'UTF-8');
    }

    return substr($fullUrl, 0, 100);
}

/**
 * Referrer URL을 100자까지 안전하게 반환하는 함수
 */
function WG_GetReferrer()
{
    $referrer = $_SERVER['HTTP_REFERER'] ?? '';

    if (function_exists('mb_substr')) {
        return mb_substr($referrer, 0, 100, 'UTF-8');
    }

    return substr($referrer, 0, 100);
}

/**
 * API URL 생성 함수
 * QueryString은 RFC3986 방식으로 URL Encoding 처리
 */
function WG_BuildApiUrl($serverIp, $params)
{
    return "https://" . $serverIp . "/?" . http_build_query($params, "", "&", PHP_QUERY_RFC3986);
}

/**
 * API URL 검증 함수
 * 서비스 도메인 패턴에 맞는지 체크
 */
function WG_IsValidApiUrl(string $url): bool
{
    if (!isset($url) || strlen($url) == 0) {
        return false;
    }

    $regEx = "/^http[s]?:\/\/\d{4,20}-\w{1,10}\.devy\.kr[\/\?].*$/i";
    return (bool)preg_match($regEx, $url);
}

/**
 * API 응답 JSON Text를 JSON 파싱하는 함수
 */
function WG_ParseJson($responseText)
{
    if (!isset($responseText) || $responseText === '') {
        return null;
    }

    $json = json_decode($responseText, true);

    if (json_last_error() !== JSON_ERROR_NONE || !is_array($json)) {
        return null;
    }

    return $json;
}

/**
 * API 호출 함수 (cURL 사용, 예외처리 포함)
 * 내부망 API 서버 전용 호출이므로 SSL 인증서 체인/Hostname 검증은 생략
 */
function WG_CallApi($url, $timeout = 10)
{
    $makeErrorJson = function($resultCode, $resultMessage) {
        return json_encode(array(
            'ResultCode'    => intval($resultCode),
            'ResultMessage' => strval($resultMessage)
        ));
    };

    try {
        if (!WG_IsValidApiUrl($url)) {
            return $makeErrorJson(-1001, 'Invalid API URL');
        }

        if (!function_exists('curl_init')) {
            return $makeErrorJson(-1002, 'cURL extension is not available');
        }

        $timeout = intval($timeout);
        if ($timeout <= 0) {
            $timeout = 10;
        }

        $connectTimeout = min(3, $timeout);

        $ch = curl_init();
        if ($ch === false) {
            return $makeErrorJson(-1003, 'Failed to initialize cURL');
        }

        curl_setopt_array($ch, array(
            CURLOPT_URL            => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => $connectTimeout,
            CURLOPT_TIMEOUT        => $timeout,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => 0,
            CURLOPT_HEADER         => false,
            CURLOPT_NOSIGNAL       => true,
            CURLOPT_USERAGENT      => 'WebGate-PHP'
        ));

        $responseText = curl_exec($ch);

        if ($responseText === false) {
            $curlErrNo  = curl_errno($ch);
            $curlErrMsg = curl_error($ch);

            if (PHP_VERSION_ID >= 80000) {
                unset($ch);
            } else {
                curl_close($ch);
            }

            return $makeErrorJson(-1100 - intval($curlErrNo), 'cURL Error: ' . $curlErrMsg);
        }

        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

        if (PHP_VERSION_ID >= 80000) {
            unset($ch);
        } else {
            curl_close($ch);
        }

        if ($httpCode < 200 || $httpCode >= 300) {
            return $makeErrorJson(-1200 - intval($httpCode), 'HTTP Error: ' . $httpCode);
        }

        return $responseText;
    } catch (Exception $e) {
        return $makeErrorJson(-1999, 'Exception: ' . $e->getMessage());
    }
}

/**
 * 문자열 자르기 함수
 * 디버그 로그 등에서 긴 문자열을 1000자 이내로 제한해서 반환
 */
function WG_LimitTrace($trace)
{
    if ($trace === null) {
        return "";
    }

    if (function_exists('mb_substr')) {
        return mb_substr($trace, 0, 1000, 'UTF-8');
    }

    return substr($trace, 0, 1000);
}
?>