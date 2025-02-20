<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="backend.aspx.cs" Inherits="Demo.backend" %>

<!DOCTYPE html>

<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title></title>
</head>

<body>
    <form id="form1" runat="server">
        <div id="app" class="container">
            <p class="title has-text-info">SAMPLE BACKEND PAGE</p>
            <div class="notification is-dark">
                <h2 class="has-text-warning">Backend 방식의 유량제어가 적용된 SAMPLE 업무페이지입니다.</h2>
                <h2 class="has-text-warning">GATE가 TEST-MODE 체크되었다면 첫 진입 시 대기표가 발급되어 대기UI가 5~10초 표시됩니다.</h2>
                <h2 class="has-text-warning">대기완료 후 재진입(F5)은 AdminPage의 FreePass 설정만큼 Pass됩니다.</h2>
                <h2 class="has-text-warning">AdminPage에서 FreePass 즉시 초기화 버튼을 클릭하면 대기표가 다시 발급되고, TEST MODE 또는 초당 유입량이 설정값을 초과하는 경우 대기가 진행됩니다.</h2>
            </div>


            <hr />
            <p class="title has-text-info is-italic">SAMPLE CONTENTS CAPTURE</p>
            <img src="img/sample-index.jpg" />
        </div>
    </form>
</body>
</html>
