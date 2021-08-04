<% OPTION EXPLICIT %>
<!--↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓여기부터 유량제어 호출코드 -->
<!--#include file="./webgate-lib.asp"-->
<%  '* ==============================================================================================
    '* 메가펜스 유량제어서비스 SAMPLE(ASP) V.210403
    '* 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
    '* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
    '* 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
    '* 작성자 : ysd@devy.co.kr
    '* ----------------------------------------------------------------------------------------------
    '* Backend 방식의 유량제어를 적용한 고객사 샘플 업무페이지 입니다.
    '* <이용 안내> 
    '*   ⊙ 아래의 샘플코드를 그대로 테스트용 페이지에 삽입해서 대기UI가 표시되는지 확인
    '*   ⊙ 서비스 세팅이 완료되면 안내받은 GATE_ID, SERVICE_ID로 수정해서 사용
    '*
    '* <주의 사항>
    '*   ⊙ 유량제어 코드는 DB접속 등의 부하량이 많은 업무로직 이전에 삽입해야 효과적입니다.
    '*   ⊙ 쿠키나 세션 등을 이용하는 간단한 처리는 유량제어 코드 이전에 배치되어도 무방합니다.
    '* ==============================================================================================

    ' SETTING
    Dim WG_GATE_ID, WG_SERVICE_ID
    WG_GATE_ID      = 1     '할당받은 GATE ID 중에서 사용
    WG_SERVICE_ID   = 9000  '고정값(fixed)

    ' 유량제어 체크
    IF WG_IsNeedToWaiting(WG_SERVICE_ID, WG_GATE_ID) THEN
        '대기해야 되는 상황이면 대기UI로 응답을 교체하고 종료
        Response.Write (WG_GetWaitingUi(WG_SERVICE_ID, WG_GATE_ID))
        Response.End '응답 끝
    END IF
%>
<!--↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑여기까지 유량제어 호출코드 -->

<!--
    여기부터 기존 업무 페이지의 시작입니다. 
    대기를 완료했거나 PASS 판정(대기할필요가 없는) 시 여기에 진입됩니다. 
    WAIT 판정(대기판정) 시 여기에 진입되지 않습니다.
-->
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title></title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <p>업무 페이지</p>
</body>
</html>
