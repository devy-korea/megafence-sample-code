<%
'/*
'* ==============================================================================================
'* 메가펜스 유량제어서비스 Backend Library for ASP (Classic / VBScript) (V26.1)
'* ==============================================================================================
'* ## 안내 ##
'*   본 라이브러리는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
'*   1. 본 라이브러리 소스는 프로젝트 Import용으로 별도의 수정 작업은 필요 없습니다.
'*   2. MSXML2.ServerXMLHTTP.6.0 사용 가능 환경에서 동작합니다.
'*   3. local pc에서는 정상적으로 동작하지 않을 수 있습니다. SSL(https) 적용된 웹서버에서 테스트를 권장합니다.
'* ----------------------------------------------------------------------------------------------
'* ## 주의 ##
'*   본 라이브러리는 데브와이 등록 특허 실시의 일부분으로 허가된 고객 및 환경 이외의 이용을 금합니다.
'*   무단 열람, 복사, 배포, 수정, 실행, 테스트 등의 행위는 권리침해 사유가 될 수 있습니다.
'* ----------------------------------------------------------------------------------------------
'* written by ysd@devy.co.kr, (c) 2024 DEVY (https://www.devy.kr)
'*/

'/////////////////////////////////
'// VER 26.1.530
'/////////////////////////////////


' 대기판정 메인함수 (V26 이후 버전부터는 이 이름 사용 권장)
Function WG_IsNeedToWait(serviceId, gateId)
    WG_IsNeedToWait = WG_IsNeedToWaiting(serviceId, gateId)
End Function


' 대기판정 메인함수 (true:대기필요, false:대기불필요)
Function WG_IsNeedToWaiting(serviceId, gateId)

    Dim WG_VER_BACKEND         : WG_VER_BACKEND         = "26.1.530"
    Dim WG_LANG_BACKEND        : WG_LANG_BACKEND        = "ASP"
    Dim WG_SERVICE_ID          : WG_SERVICE_ID          = serviceId
    Dim WG_GATE_ID             : WG_GATE_ID             = gateId
    Dim WG_MAX_TRY_COUNT       : WG_MAX_TRY_COUNT       = 3
    Dim WG_IS_CHECKOUT_OK      : WG_IS_CHECKOUT_OK      = False
    Dim WG_GATE_SERVER_MAX     : WG_GATE_SERVER_MAX     = 6
    Dim WG_GATE_SERVERS        : WG_GATE_SERVERS        = Array()
    Dim WG_TOKEN_NO            : WG_TOKEN_NO            = ""
    Dim WG_TOKEN_STATE         : WG_TOKEN_STATE         = ""
    Dim WG_TOKEN_KEY           : WG_TOKEN_KEY           = ""
    Dim WG_WAS_IP              : WG_WAS_IP              = ""
    Dim WG_TRACE               : WG_TRACE               = "WG_IsNeedToWaiting()::"
    Dim WG_TRACE_LEVEL         : WG_TRACE_LEVEL         = 0
    Dim WG_IS_LOADTEST         : WG_IS_LOADTEST         = "N"
    Dim WG_REQ_PAGE            : WG_REQ_PAGE            = WG_GetRequestPageUrl()
    Dim WG_REQ_IP              : WG_REQ_IP              = WG_Nz(Request.ServerVariables("REMOTE_ADDR"))
    Dim WG_REFERRER            : WG_REFERRER            = WG_GetReferrer()
    Dim WG_CLIENT_IP           : WG_CLIENT_IP           = WG_GetUserIpAddr()
    Dim WG_OUT_COUNT           : WG_OUT_COUNT           = 0
    Dim WG_RESULT_CODE         : WG_RESULT_CODE         = 0
    Dim WG_RESULT_MESSAGE      : WG_RESULT_MESSAGE      = ""
    Dim WG_GATE_OPERATION_MODE : WG_GATE_OPERATION_MODE = "GATE"
    Dim WG_IS_NEED_TO_WAIT     : WG_IS_NEED_TO_WAIT     = False
    Dim WG_RETURN_FLAG         : WG_RETURN_FLAG         = False

    Dim i, tryCount, apiUrl, responseText, json
    Dim traceLevelText

    ' get trace level from cookie
    traceLevelText = Request.Cookies("WG_TRACE_LEVEL")
    If Len(WG_Nz(traceLevelText)) > 0 And IsNumeric(traceLevelText) Then
        WG_TRACE_LEVEL = CLng(traceLevelText)
    End If

    ' init was ip list
    ReDim WG_GATE_SERVERS(WG_GATE_SERVER_MAX - 1)
    For i = 0 To WG_GATE_SERVER_MAX - 1
        WG_GATE_SERVERS(i) = WG_SERVICE_ID & "-" & i & ".devy.kr"
    Next

    ' deprecated parameter for load test, will be removed in future version
    If Request.QueryString("IsLoadTest") = "Y" Then
        WG_IS_LOADTEST = "Y"
    End If

    '******************************************************************************
    ' STEP-1 : URL Parameter로 대기표 검증 (WG_TOKEN url param이 있으면 처리)
    '******************************************************************************
    On Error Resume Next
    WG_TRACE = WG_TRACE & "STEP1:"

    Dim tokenParam : tokenParam = Request.QueryString("WG_TOKEN")
    If Len(WG_Nz(tokenParam)) > 0 Then
        Dim pv : pv = Split(tokenParam, ",")
        If UBound(pv) = 3 Then
            Dim paramGateId, paramTokenNo, paramTokenKey, paramWasIp
            paramGateId   = pv(0)
            paramTokenNo  = pv(1)
            paramTokenKey = pv(2)
            paramWasIp    = pv(3)

            If Len(WG_Nz(paramTokenNo)) > 0 And Len(WG_Nz(paramTokenKey)) > 0 _
               And Len(WG_Nz(paramWasIp)) > 0 And StrComp(paramGateId, WG_GATE_ID) = 0 Then

                ' API Call by ACTION=OUT
                If WG_RETURN_FLAG = False Then
                    WG_TRACE = WG_TRACE & "API_OUT:"
                    apiUrl = WG_BuildApiUrl(paramWasIp, WG_SERVICE_ID, WG_GATE_ID, "OUT", _
                             paramTokenNo, paramTokenKey, WG_CLIENT_IP, WG_REQ_PAGE, WG_IS_LOADTEST)

                    If WG_TRACE_LEVEL >= 2 Then WG_TRACE = WG_TRACE & apiUrl & ","

                    responseText = WG_CallApi(apiUrl, 10)

                    WG_RESULT_CODE    = WG_JsonGetInt(responseText, "ResultCode", 0)
                    WG_RESULT_MESSAGE = WG_JsonGetStr(responseText, "ResultMessage")

                    WG_TRACE = WG_TRACE & WG_RESULT_CODE & "/" & WG_GATE_OPERATION_MODE
                    If WG_TRACE_LEVEL >= 2 Then WG_TRACE = WG_TRACE & ":" & WG_RESULT_MESSAGE & ","
                    WG_TRACE = WG_TRACE & ","

                    ' 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                    If WG_RESULT_CODE = 0 Then
                        WG_WAS_IP              = paramWasIp
                        WG_TOKEN_KEY           = paramTokenKey
                        WG_TOKEN_STATE         = WG_JsonGetStr(responseText, "TokenState")
                        WG_GATE_OPERATION_MODE = WG_JsonGetStrDef(responseText, "GateOperationMode", "GATE")
                        WG_TOKEN_NO            = WG_JsonGetStr(responseText, "TokenNo")
                        WG_OUT_COUNT           = WG_JsonGetInt(responseText, "OutCount", 0)

                        If WG_GATE_OPERATION_MODE = "ALERT" Then
                            WG_TRACE = WG_TRACE & "ALERT,"
                            WG_RETURN_FLAG     = True
                            WG_IS_CHECKOUT_OK  = False
                            WG_IS_NEED_TO_WAIT = True
                        ElseIf WG_GATE_OPERATION_MODE = "GATE" Then
                            If WG_TOKEN_STATE = "WAIT" Then
                                WG_TRACE = WG_TRACE & "WAIT,"
                                WG_RETURN_FLAG     = True
                                WG_IS_CHECKOUT_OK  = False
                                WG_IS_NEED_TO_WAIT = True
                            ElseIf WG_TOKEN_STATE = "CONNECT" Or WG_TOKEN_STATE = "IN" Or WG_TOKEN_STATE = "OUT" Then
                                WG_TRACE = WG_TRACE & "PASS,"
                                WG_RETURN_FLAG     = True
                                WG_IS_CHECKOUT_OK  = True
                                WG_IS_NEED_TO_WAIT = False
                            Else
                                WG_TRACE = WG_TRACE & "FAIL1[TokenState:" & WG_TOKEN_STATE & "],"
                            End If
                        Else
                            WG_TRACE = WG_TRACE & "FAIL2[GateOperationMode:" & WG_GATE_OPERATION_MODE & "],"
                        End If
                    Else
                        WG_TRACE = WG_TRACE & "FAIL3[ResultCode:" & WG_RESULT_CODE & "],"
                    End If
                End If
            Else
                WG_TRACE = WG_TRACE & "SKIP1,"
            End If
        Else
            WG_TRACE = WG_TRACE & "SKIP2,"
        End If
    Else
        WG_TRACE = WG_TRACE & "SKIP3,"
    End If

    If Err <> 0 Then
        WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
        Err.Clear
    End If
    On Error GoTo 0

    '******************************************************************************
    ' STEP-2 : Cookie로 대기표 검증
    '******************************************************************************
    On Error Resume Next
    WG_TRACE = WG_TRACE & "→STEP2:"

    If WG_RETURN_FLAG = False Then
        Dim cookieTokenNo, cookieTokenKey, cookieWasIp
        cookieTokenNo  = WG_ReadCookie("WG_TOKEN_NO")
        cookieTokenKey = WG_ReadCookie("WG_CLIENT_ID")
        cookieWasIp    = WG_ReadCookie("WG_WAS_IP")

        If WG_TRACE_LEVEL >= 1 Then
            WG_TRACE = WG_TRACE & "WG_TOKEN_NO:"  & WG_NvlText(cookieTokenNo) & "|" _
                                & "WG_TOKEN_KEY:" & WG_NvlText(cookieTokenKey) & "|" _
                                & "WG_WAS_IP:"    & WG_NvlText(cookieWasIp) & ","
        End If

        ' WG_TOKEN_NO, WG_CLIENT_ID, WG_WAS_IP 쿠키가 모두 있을 때 API Call 시도
        If Len(cookieTokenNo) > 0 And Len(cookieTokenKey) > 0 And Len(cookieWasIp) > 0 Then

            ' API ACTION=OUT
            If WG_IS_CHECKOUT_OK = False And WG_IS_NEED_TO_WAIT = False Then
                WG_TRACE = WG_TRACE & "API_OUT:"
                apiUrl = WG_BuildApiUrl(cookieWasIp, WG_SERVICE_ID, WG_GATE_ID, "OUT", _
                         cookieTokenNo, cookieTokenKey, WG_CLIENT_IP, WG_REQ_PAGE, WG_IS_LOADTEST)

                If WG_TRACE_LEVEL >= 2 Then WG_TRACE = WG_TRACE & apiUrl & ","

                responseText = WG_CallApi(apiUrl, 10)

                WG_RESULT_CODE    = WG_JsonGetInt(responseText, "ResultCode", 0)
                WG_RESULT_MESSAGE = WG_JsonGetStr(responseText, "ResultMessage")

                WG_TRACE = WG_TRACE & WG_RESULT_CODE & "/" & WG_GATE_OPERATION_MODE
                If WG_TRACE_LEVEL >= 2 Then WG_TRACE = WG_TRACE & ":" & WG_RESULT_MESSAGE & ","
                WG_TRACE = WG_TRACE & ","

                ' 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
                If WG_RESULT_CODE = 0 Then
                    WG_WAS_IP              = cookieWasIp
                    WG_TOKEN_KEY           = cookieTokenKey
                    WG_TOKEN_STATE         = WG_JsonGetStr(responseText, "TokenState")
                    WG_GATE_OPERATION_MODE = WG_JsonGetStrDef(responseText, "GateOperationMode", "GATE")
                    WG_TOKEN_NO            = WG_JsonGetStr(responseText, "TokenNo")
                    WG_OUT_COUNT           = WG_JsonGetInt(responseText, "OutCount", 0)

                    If WG_GATE_OPERATION_MODE = "ALERT" Then
                        WG_TRACE = WG_TRACE & "ALERT,"
                        WG_RETURN_FLAG     = True
                        WG_IS_CHECKOUT_OK  = False
                        WG_IS_NEED_TO_WAIT = True
                    ElseIf WG_GATE_OPERATION_MODE = "GATE" Then
                        If WG_TOKEN_STATE = "WAIT" Then
                            WG_TRACE = WG_TRACE & "WAIT,"
                            WG_RETURN_FLAG     = True
                            WG_IS_CHECKOUT_OK  = False
                            WG_IS_NEED_TO_WAIT = True
                        ElseIf WG_TOKEN_STATE = "CONNECT" Or WG_TOKEN_STATE = "IN" Or WG_TOKEN_STATE = "OUT" Then
                            WG_TRACE = WG_TRACE & "PASS,"
                            WG_RETURN_FLAG     = True
                            WG_IS_CHECKOUT_OK  = True
                            WG_IS_NEED_TO_WAIT = False
                        Else
                            WG_TRACE = WG_TRACE & "FAIL1[TokenState:" & WG_TOKEN_STATE & "],"
                        End If
                    Else
                        WG_TRACE = WG_TRACE & "FAIL2[GateOperationMode:" & WG_GATE_OPERATION_MODE & "],"
                    End If
                Else
                    WG_TRACE = WG_TRACE & "FAIL3[ResultCode:" & WG_RESULT_CODE & "],"
                End If
            End If
        Else
            WG_TRACE = WG_TRACE & "SKIP1,"
        End If
    Else
        WG_TRACE = WG_TRACE & "SKIP2,"
    End If

    If Err <> 0 Then
        WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
        Err.Clear
    End If
    On Error GoTo 0

    '******************************************************************************
    ' STEP-3 : 신규접속자로 간주하고 대기열 표시여부 판단
    '******************************************************************************
    WG_TRACE = WG_TRACE & "→STEP3:"
    tryCount = 0

    If WG_RETURN_FLAG = False Then
        WG_RESULT_CODE = -1

        Dim serverCount, drawResult, serverIp
        serverCount = WG_GATE_SERVER_MAX
        Randomize
        drawResult = Int(serverCount * Rnd)

        ' TokenKey 쿠키(WG_CLIENT_ID)가 없으면 신규 발급(채번)
        WG_TOKEN_KEY = WG_ReadCookie("WG_CLIENT_ID")
        If Len(WG_TOKEN_KEY) = 0 Then
            WG_TOKEN_KEY = WG_GetRandomString(12)
            If WG_TRACE_LEVEL >= 1 Then WG_TRACE = WG_TRACE & "Generate TOKEN:" & WG_TOKEN_KEY & ","
            WG_WriteCookie "WG_CLIENT_ID", WG_TOKEN_KEY
        End If

        ' 최대 3회 Api call (action=MATCHING) 시도
        For tryCount = 0 To WG_MAX_TRY_COUNT - 1
            On Error Resume Next
            WG_TRACE = WG_TRACE & "API_MATCHING" & (tryCount + 1) & ":"

            If tryCount = 0 And Len(WG_WAS_IP) > 0 Then
                serverIp = WG_WAS_IP
            Else
                serverIp = WG_GATE_SERVERS(drawResult Mod serverCount)
                WG_WAS_IP = serverIp
                drawResult = drawResult + 1
            End If

            apiUrl = WG_BuildApiUrl(serverIp, WG_SERVICE_ID, WG_GATE_ID, "MATCHING", _
                     "", WG_TOKEN_KEY, WG_CLIENT_IP, WG_REQ_PAGE, WG_IS_LOADTEST)

            responseText = WG_CallApi(apiUrl, 3 * (tryCount + 1))

            WG_RESULT_CODE    = WG_JsonGetInt(responseText, "ResultCode", 0)
            WG_RESULT_MESSAGE = WG_JsonGetStr(responseText, "ResultMessage")

            WG_TRACE = WG_TRACE & ":" & WG_RESULT_CODE & "/" & WG_GATE_OPERATION_MODE & ","
            If WG_TRACE_LEVEL >= 2 Then WG_TRACE = WG_TRACE & WG_RESULT_MESSAGE & ","
            WG_TRACE = WG_TRACE & ","

            ' 정상 대기완료 토큰 조건 : ResultCode==0 && GateOperationMode=="GATE" && TokenState in ("CONNECT","IN","OUT")
            If WG_RESULT_CODE = 0 Then
                WG_TOKEN_STATE         = WG_JsonGetStr(responseText, "TokenState")
                WG_GATE_OPERATION_MODE = WG_JsonGetStrDef(responseText, "GateOperationMode", "GATE")
                WG_TOKEN_NO            = WG_JsonGetStr(responseText, "TokenNo")
                WG_OUT_COUNT           = WG_JsonGetInt(responseText, "OutCount", 0)

                If WG_GATE_OPERATION_MODE = "ALERT" Then
                    WG_TRACE = WG_TRACE & "ALERT,"
                    WG_RETURN_FLAG     = True
                    WG_IS_CHECKOUT_OK  = False
                    WG_IS_NEED_TO_WAIT = True
                    If Err <> 0 Then Err.Clear
                    On Error GoTo 0
                    Exit For
                ElseIf WG_GATE_OPERATION_MODE = "GATE" Then
                    If WG_TOKEN_STATE = "WAIT" Then
                        WG_TRACE = WG_TRACE & "WAIT,"
                        WG_RETURN_FLAG     = True
                        WG_IS_CHECKOUT_OK  = False
                        WG_IS_NEED_TO_WAIT = True
                        If Err <> 0 Then Err.Clear
                        On Error GoTo 0
                        Exit For
                    ElseIf WG_TOKEN_STATE = "CONNECT" Or WG_TOKEN_STATE = "IN" Or WG_TOKEN_STATE = "OUT" Then
                        WG_TRACE = WG_TRACE & "PASS,"
                        WG_RETURN_FLAG     = True
                        WG_IS_CHECKOUT_OK  = True
                        WG_IS_NEED_TO_WAIT = False
                        If Err <> 0 Then Err.Clear
                        On Error GoTo 0
                        Exit For
                    Else
                        WG_TRACE = WG_TRACE & "FAIL1[TokenState:" & WG_TOKEN_STATE & "],"
                        If Err <> 0 Then Err.Clear
                        On Error GoTo 0
                        Exit For
                    End If
                Else
                    WG_TRACE = WG_TRACE & "FAIL2[GateOperationMode:" & WG_GATE_OPERATION_MODE & "],"
                End If
            Else
                WG_TRACE = WG_TRACE & "FAIL3[ResultCode:" & WG_RESULT_CODE & "],"
            End If

            If Err <> 0 Then
                WG_TRACE = WG_TRACE & "ERROR:" & Err.Description & ","
                Err.Clear
            End If
            On Error GoTo 0
        Next

        If tryCount >= WG_MAX_TRY_COUNT Then
            WG_TRACE = WG_TRACE & "RETRY_EXCEEDED,"
            If WG_RESULT_CODE < 0 Then
                WG_IS_NEED_TO_WAIT = False
            Else
                WG_IS_NEED_TO_WAIT = True
            End If
        End If
    Else
        WG_TRACE = WG_TRACE & "SKIP,"
    End If

    WG_TRACE = WG_TRACE & "TryCount:" & tryCount & ","

    Dim result : result = WG_IS_NEED_TO_WAIT

    WG_TRACE = WG_TRACE & "→returns:" & WG_BoolText(result) & "," & WG_RESULT_CODE & "," & WG_RESULT_MESSAGE
    WG_TRACE = WG_LimitTrace(WG_TRACE)

    ' 항상 생성하는 쿠키
    WG_WriteCookie "WG_TIME",     WG_UtcTimeFormat(Now())
    WG_WriteCookie "WG_GATE_ID",  WG_GATE_ID
    WG_WriteCookie "WG_WAS_IP",   WG_WAS_IP
    WG_WriteCookie "WG_TOKEN_NO", WG_TOKEN_NO
    WG_WriteCookie "WG_OUT_COUNT", CStr(WG_OUT_COUNT)

    ' 조건부 삭제 쿠키
    WG_DeleteCookie "WG_TRACE"
    WG_DeleteCookie "WG_LANG_BACKEND"
    WG_DeleteCookie "WG_VER_BACKEND"
    WG_DeleteCookie "WG_REQ_PAGE"
    WG_DeleteCookie "WG_REQ_IP"
    WG_DeleteCookie "WG_REFERRER"
    WG_DeleteCookie "WG_CLIENT_IP"
    WG_DeleteCookie "WG_TOKEN_STATE"
    WG_DeleteCookie "WG_RESULT_CODE"
    WG_DeleteCookie "WG_GATE_OPERATION_MODE"

    If WG_TRACE_LEVEL >= 2 Then
        WG_WriteCookie "WG_TRACE",               WG_TRACE
        WG_WriteCookie "WG_REQ_PAGE",            WG_REQ_PAGE
        WG_WriteCookie "WG_REQ_IP",              WG_REQ_IP
        WG_WriteCookie "WG_GATE_OPERATION_MODE", WG_GATE_OPERATION_MODE
        WG_WriteCookie "WG_REFERRER",            WG_REFERRER
        WG_WriteCookie "WG_CLIENT_IP",           WG_CLIENT_IP
        WG_WriteCookie "WG_TOKEN_STATE",         WG_TOKEN_STATE
        WG_WriteCookie "WG_RESULT_CODE",         CStr(WG_RESULT_CODE)
    End If

    If WG_TRACE_LEVEL >= 1 Then
        WG_WriteCookie "WG_LANG_BACKEND", WG_LANG_BACKEND
        WG_WriteCookie "WG_VER_BACKEND",  WG_VER_BACKEND
        WG_WriteCookie "WG_TRACE",        WG_TRACE
    End If

    If WG_TRACE_LEVEL <= 0 Then
        WG_DeleteCookie "WG_TRACE_LEVEL"
    End If

    WG_IsNeedToWaiting = result
End Function


' 호환성 유지. WG_IsNeedToWaiting()만 사용
Function WG_IsNeedToWaiting_V2(serviceId, gateId)
    WG_IsNeedToWaiting_V2 = WG_IsNeedToWaiting(serviceId, gateId)
End Function


' 호환성 유지. WG_IsNeedToWaiting()만 사용
Function WG_IsValidToken(serviceId, gateId)
    WG_IsValidToken = Not WG_IsNeedToWaiting(serviceId, gateId)
End Function


' 대기UI HTML return
Function WG_GetWaitingUi(serviceId, gateId)
    Dim versionTag, html
    versionTag = WG_VersionTag()

    html = "<!DOCTYPE html>"                                                                                                                  & vbCrLf &_
           "<html>"                                                                                                                           & vbCrLf &_
           "<head>"                                                                                                                           & vbCrLf &_
           "    <meta http-equiv='X-UA-Compatible' content='IE=edge'/>"                                                                       & vbCrLf &_
           "    <meta charset='utf-8'/>"                                                                                                       & vbCrLf &_
           "    <meta http-equiv='cache-control' content='no-cache' />"                                                                        & vbCrLf &_
           "    <meta http-equiv='Expires' content='-1'/>"                                                                                     & vbCrLf &_
           "    <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'/>" & vbCrLf &_
           "    <meta name='robots' content='noindex,nofollow'>"                                                                               & vbCrLf &_
           "    <title></title>"                                                                                                               & vbCrLf &_
           "</head>"                                                                                                                           & vbCrLf &_
           "<body>"                                                                                                                            & vbCrLf &_
           "    <link href='https://cdn2.devy.kr/WG_SERVICE_ID/css/webgate.css?v=" & versionTag & "' rel='stylesheet' />"                      & vbCrLf &_
           "    <script type='text/javascript' src='https://cdn2.devy.kr/WG_SERVICE_ID/js/webgate.js?v=" & versionTag & "'></script>"          & vbCrLf &_
           "    <script>"                                                                                                                      & vbCrLf &_
           "        function WG_PageLoaded() {"                                                                                                & vbCrLf &_
           "            var gateId = 'WG_GATE_ID';"                                                                                            & vbCrLf &_
           "            var nextUrl = window.location.href;"                                                                                   & vbCrLf &_
           "            var config = {"                                                                                                        & vbCrLf &_
           "                gateId: gateId,"                                                                                                   & vbCrLf &_
           "                uiMode: 'BACKEND',"                                                                                                & vbCrLf &_
           "                topTitle: '서비스 접속 대기 중',"                                                                                  & vbCrLf &_
           "                uiShowDelay: 0 * 1000,"                                                                                            & vbCrLf &_
           "                uiHideDelay: 10 * 1000,"                                                                                           & vbCrLf &_
           "                resetForce: false,"                                                                                               & vbCrLf &_
           "                customType: '',"                                                                                                   & vbCrLf &_
           "                onSuccess: function(data) {"                                                                                       & vbCrLf &_
           "                    var res = $WG.lastResponse;"                                                                                    & vbCrLf &_
           "                    if (res.GateOperationMode == 'ALERT') {"                                                                       & vbCrLf &_
           "                        if (res.GateOperationMessageTitle == 'NOT_OPEN') {"                                                        & vbCrLf &_
           "                            alert(res.GateOperationMessageDetail);"                                                                & vbCrLf &_
           "                        }"                                                                                                         & vbCrLf &_
           "                    } else if (res.GateOperationMode == 'GATE') {"                                                                 & vbCrLf &_
           "                        WG_ChangeUrl(nextUrl);"                                                                                    & vbCrLf &_
           "                    }"                                                                                                             & vbCrLf &_
           "                },"                                                                                                                & vbCrLf &_
           "                onMdsDetected: function(data) {"                                                                                   & vbCrLf &_
           "                    if (typeof turnstile != 'undefined' && turnstile != null) {"                                                   & vbCrLf &_
           "                        turnstile.reset();"                                                                                        & vbCrLf &_
           "                    }"                                                                                                             & vbCrLf &_
           "                    var msg = '매크로 등에 의한 부정접속 시도가 감지되었습니다.\n만일 \'사람인지 확인하십시오\' 문구가 화면에 표시되었다면 확인란을 체크하신 후 다시 시도해 보시기 바랍니다.';" & vbCrLf &_
           "                    if (data.ResultMessage == 'MDS_FAIL_BLOCKED_IP') {"                                                            & vbCrLf &_
           "                        msg = '접속 차단된 IP입니다.\n다른 IP에서 접속하거나 관리자에게 문의 주시기 바랍니다.';"                    & vbCrLf &_
           "                    }"                                                                                                             & vbCrLf &_
           "                    alert(msg);"                                                                                                   & vbCrLf &_
           "                    return;"                                                                                                       & vbCrLf &_
           "                },"                                                                                                                & vbCrLf &_
           "                onAlert: function(data) {"                                                                                         & vbCrLf &_
           "                    var res = $WG.lastResponse;"                                                                                    & vbCrLf &_
           "                    alert(res.GateOperationMessageDetail);"                                                                        & vbCrLf &_
           "                },"                                                                                                                & vbCrLf &_
           "                onWaiting: function(data) {"                                                                                       & vbCrLf &_
           "                },"                                                                                                                & vbCrLf &_
           "                onFail: function(data) {"                                                                                          & vbCrLf &_
           "                    alert('죄송합니다. 잠시 후 다시 시도해 주세요');"                                                              & vbCrLf &_
           "                },"                                                                                                                & vbCrLf &_
           "                onFinally: function() {"                                                                                           & vbCrLf &_
           "                }"                                                                                                                 & vbCrLf &_
           "            };"                                                                                                                    & vbCrLf &_
           "            WG_StartWebGate(config);"                                                                                              & vbCrLf &_
           "        }"                                                                                                                         & vbCrLf &_
           "    </script>"                                                                                                                     & vbCrLf &_
           "</body>"                                                                                                                           & vbCrLf &_
           "</html>"

    html = Replace(html, "WG_SERVICE_ID", serviceId)
    html = Replace(html, "WG_GATE_ID", gateId)

    WG_GetWaitingUi = html
End Function


'======================================================================
' Private Helpers
'======================================================================

' API URL 생성 (TokenNo/TokenKey/ClientIp/ReqPage는 URL 인코딩)
Function WG_BuildApiUrl(wasIp, serviceId, gateId, action, tokenNo, tokenKey, clientIp, reqPage, isLoadTest)
    WG_BuildApiUrl = "https://" & wasIp & "/?ServiceId=" & serviceId _
        & "&GateId=" & gateId _
        & "&Action=" & action _
        & "&TokenNo=" & WG_EncodeURIComponent(tokenNo) _
        & "&TokenKey=" & WG_EncodeURIComponent(tokenKey) _
        & "&ClientIp=" & WG_EncodeURIComponent(clientIp) _
        & "&ReqPage=" & WG_EncodeURIComponent(reqPage) _
        & "&IsLoadTest=" & isLoadTest
End Function


' API call 후 응답(JSON Text) 반환. 오류 시 ErrorJson 반환
Function WG_CallApi(url, timeoutSeconds)
    Dim t, ms, connMs, httpCode, respText, XmlHttp, em

    If Not WG_IsValidApiUrl(url) Then
        WG_CallApi = WG_MakeErrorJson(-1001, "Invalid API URL")
        Exit Function
    End If

    t = timeoutSeconds
    If t <= 0 Then t = 10
    ms = t * 1000
    connMs = 3000
    If connMs > ms Then connMs = ms

    On Error Resume Next
    Set XmlHttp = Server.CreateObject("MSXML2.ServerXMLHTTP.6.0")
    XmlHttp.SetTimeouts connMs, connMs, ms, ms
    XmlHttp.open "GET", url, False
    XmlHttp.setRequestHeader "User-Agent", "WebGate-ASP"
    XmlHttp.setRequestHeader "Accept", "application/json,text/plain,*/*"
    XmlHttp.send

    If Err.Number <> 0 Then
        em = Err.Description
        Err.Clear
        On Error GoTo 0
        Set XmlHttp = Nothing
        WG_CallApi = WG_MakeErrorJson(-1999, "Exception: " & em)
        Exit Function
    End If
    On Error GoTo 0

    httpCode = XmlHttp.status
    respText = XmlHttp.responseText
    Set XmlHttp = Nothing

    If httpCode < 200 Or httpCode >= 300 Then
        WG_CallApi = WG_MakeErrorJson(-1200 - httpCode, "HTTP Error: " & httpCode)
        Exit Function
    End If

    If Len(WG_Nz(respText)) = 0 Then
        WG_CallApi = WG_MakeErrorJson(-1300, "Invalid JSON response")
        Exit Function
    End If

    WG_CallApi = respText
End Function


Function WG_MakeErrorJson(code, msg)
    Dim m : m = WG_Nz(msg)
    m = Replace(m, "\", "\\")
    m = Replace(m, """", "\""")
    m = Replace(m, vbCr, " ")
    m = Replace(m, vbLf, " ")
    WG_MakeErrorJson = "{""ResultCode"":" & code & ",""ResultMessage"":""" & m & """}"
End Function


' JSON 문자열에서 key의 값을 String으로 추출 (string 값 우선, 없으면 numeric/bool)
Function WG_JsonGetStr(jsonText, key)
    Dim rx, matches
    WG_JsonGetStr = ""
    If Len(WG_Nz(jsonText)) = 0 Then Exit Function

    Set rx = New RegExp
    rx.IgnoreCase = True
    rx.Global = False

    ' string value: "key" : "value"
    rx.Pattern = """" & key & """\s*:\s*""([^""]*)"""
    Set matches = rx.Execute(jsonText)
    If matches.Count > 0 Then
        WG_JsonGetStr = matches(0).SubMatches(0)
        Set rx = Nothing
        Exit Function
    End If

    ' numeric/bool value: "key" : 123
    rx.Pattern = """" & key & """\s*:\s*([^,}\s""]+)"
    Set matches = rx.Execute(jsonText)
    If matches.Count > 0 Then
        WG_JsonGetStr = matches(0).SubMatches(0)
    End If

    Set rx = Nothing
End Function


Function WG_JsonGetStrDef(jsonText, key, defaultVal)
    Dim v : v = WG_JsonGetStr(jsonText, key)
    If Len(v) = 0 Then
        WG_JsonGetStrDef = defaultVal
    Else
        WG_JsonGetStrDef = v
    End If
End Function


Function WG_JsonGetInt(jsonText, key, defaultVal)
    Dim v : v = WG_JsonGetStr(jsonText, key)
    If Len(v) > 0 And IsNumeric(v) Then
        WG_JsonGetInt = CLng(v)
    Else
        WG_JsonGetInt = defaultVal
    End If
End Function


' Random key 생성 (WG_CLIENT_ID용) - 체크코드 2자리 포함
Function WG_GetRandomString(length)
    Dim characters : characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    Dim charLen : charLen = Len(characters)
    Dim s : s = ""
    Dim i, c, minChar, maxChar, minPos, maxPos, minCheck, maxCheck

    If length < 12 Then length = 12
    Randomize

    ' 본문 생성 (뒤 2자리는 체크코드)
    For i = 1 To (length - 2)
        s = s & Mid(characters, Int(charLen * Rnd) + 1, 1)
    Next

    minChar = Mid(s, 1, 1)
    maxChar = Mid(s, 1, 1)
    For i = 2 To Len(s)
        c = Mid(s, i, 1)
        If c < minChar Then minChar = c
        If c > maxChar Then maxChar = c
    Next

    minPos = InStr(characters, minChar) - 1   ' 0-based
    maxPos = InStr(characters, maxChar) - 1

    minCheck = Mid(characters, ((minPos - 1 + charLen) Mod charLen) + 1, 1)
    maxCheck = Mid(characters, ((maxPos + 1 + charLen) Mod charLen) + 1, 1)

    WG_GetRandomString = s & minCheck & maxCheck
End Function


' Read Cookie : URL Decoding (Request.Cookies가 자동 디코딩)
Function WG_ReadCookie(key)
    WG_ReadCookie = WG_Nz(Request.Cookies(key))
End Function


' Write Cookie : URL Encoding, SameSite=Lax. WG_CLIENT_ID는 365일, 나머지는 1일
Function WG_WriteCookie(key, value)
    Dim days : days = 1
    If key = "WG_CLIENT_ID" Then days = 365
    Dim v : v = Server.URLEncode(WG_Nz(value))
    Dim cookieText
    cookieText = key & "=" & v & "; Max-Age=" & (86400 * days) & "; Path=/; SameSite=Lax"
    ' cookieText = cookieText & "; Secure"   ' local test 지원 위해 주석 처리
    Response.AddHeader "Set-Cookie", cookieText
End Function


' Delete cookie
Function WG_DeleteCookie(key)
    Response.AddHeader "Set-Cookie", key & "=; Max-Age=0; Path=/; SameSite=Lax"
End Function


' SSRF 대응용 API URL 검증
Function WG_IsValidApiUrl(url)
    Dim rx
    If Len(WG_Nz(url)) = 0 Then
        WG_IsValidApiUrl = False
        Exit Function
    End If
    Set rx = New RegExp
    rx.Pattern    = "^https?://[0-9]{4,20}-[A-Za-z0-9_]{1,10}\.devy\.kr[/?].*$"
    rx.IgnoreCase = True
    rx.Global     = False
    WG_IsValidApiUrl = rx.Test(url)
    Set rx = Nothing
End Function


' 사용자(Browser) IP를 X-Forwarded-For 헤더 기반으로 구하는 함수
Function WG_GetUserIpAddr()
    Dim remoteAddr, xff, ip
    remoteAddr = WG_Nz(Request.ServerVariables("REMOTE_ADDR"))
    xff = WG_Nz(Request.ServerVariables("HTTP_X_FORWARDED_FOR"))
    If Len(xff) > 0 Then
        ip = Trim(Split(xff, ",")(0))
        If WG_IsValidIp(ip) Then
            WG_GetUserIpAddr = ip
        Else
            WG_GetUserIpAddr = remoteAddr
        End If
    Else
        WG_GetUserIpAddr = remoteAddr
    End If
End Function


Function WG_IsValidIp(ip)
    Dim rx
    If Len(WG_Nz(ip)) = 0 Then
        WG_IsValidIp = False
        Exit Function
    End If
    Set rx = New RegExp
    rx.IgnoreCase = True
    rx.Global = False
    ' IPv4 또는 IPv6(간이) 패턴
    rx.Pattern = "^(([0-9]{1,3}\.){3}[0-9]{1,3})$|^([0-9A-Fa-f:]+)$"
    WG_IsValidIp = rx.Test(ip)
    Set rx = Nothing
End Function


' 요청페이지 URL(100자 이내)
Function WG_GetRequestPageUrl()
    Dim scheme, host, uri, qs, fullUrl
    scheme = WG_Nz(Request.ServerVariables("HTTP_X_FORWARDED_PROTO"))
    If Len(scheme) = 0 Then
        If LCase(WG_Nz(Request.ServerVariables("HTTPS"))) = "on" Then
            scheme = "https"
        Else
            scheme = "http"
        End If
    End If
    host = WG_Nz(Request.ServerVariables("HTTP_HOST"))
    uri  = WG_Nz(Request.ServerVariables("URL"))
    qs   = WG_Nz(Request.ServerVariables("QUERY_STRING"))
    fullUrl = scheme & "://" & host & uri
    If Len(qs) > 0 Then fullUrl = fullUrl & "?" & qs
    If Len(fullUrl) > 100 Then fullUrl = Left(fullUrl, 100)
    WG_GetRequestPageUrl = fullUrl
End Function


' Referrer URL(100자 이내)
Function WG_GetReferrer()
    Dim r : r = WG_Nz(Request.ServerVariables("HTTP_REFERER"))
    If Len(r) > 100 Then r = Left(r, 100)
    WG_GetReferrer = r
End Function


Function WG_EncodeURIComponent(v)
    WG_EncodeURIComponent = Server.URLEncode(WG_Nz(v))
End Function


Function WG_LimitTrace(t)
    If Len(WG_Nz(t)) > 1000 Then
        WG_LimitTrace = Left(t, 1000)
    Else
        WG_LimitTrace = WG_Nz(t)
    End If
End Function


' yyyyMMddHH00 형식 캐시버전 태그
Function WG_VersionTag()
    Dim y, mo, d, h
    y  = Year(Now())
    mo = Right("0" & Month(Now()), 2)
    d  = Right("0" & Day(Now()), 2)
    h  = Right("0" & Hour(Now()), 2)
    WG_VersionTag = y & mo & d & h & "00"
End Function


Function WG_UtcTimeFormat(dt)
    WG_UtcTimeFormat = Year(dt) & "-" & Right("0" & Month(dt), 2) & "-" & Right("0" & Day(dt), 2) _
        & "T" & Right("0" & Hour(dt), 2) & ":" & Right("0" & Minute(dt), 2) & ":" & Right("0" & Second(dt), 2) & "+09:00"
End Function


' Null/Empty 안전 문자열 변환
Function WG_Nz(v)
    If IsNull(v) Or IsEmpty(v) Then
        WG_Nz = ""
    Else
        WG_Nz = CStr(v)
    End If
End Function


Function WG_NvlText(v)
    If Len(WG_Nz(v)) > 0 Then
        WG_NvlText = v
    Else
        WG_NvlText = "NULL"
    End If
End Function


Function WG_BoolText(b)
    If b Then
        WG_BoolText = "true"
    Else
        WG_BoolText = "false"
    End If
End Function
%>
