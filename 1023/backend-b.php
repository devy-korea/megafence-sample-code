<?php

if($_POST) {
    echo "<h3>정상적으로 처리되었습니다.</h3>";
    echo '<pre>', print_r($_POST, 1), '</pre>';
    echo '<div><a href="">돌아가기</a></div>';
    exit;
}

define('WG_GATE_ID', 278);       // back-end 사용 시 GATE ID
define('WG_FORM_GATE_ID', 279);  // front-end 사용 시 GATE ID

include('webgate-b.php');

?>
<!DOCTYPE html>
<html>
<head>
    <style>
    body { padding: 20px; }
    </style>
</head>
<body>
    <div>
        <form method="post" action="test-frontend.php">
            <div>이름 <input type="text" name="name" value=""></div>
            <div>번호 <input type="text" name="license_num"></div>

            <!----------------------------------------------------------------
            type="button"추가. 버튼클릭 시 자동submit 방지
            ------------------------------------------------------------------>
            <button id="submit-button" type="button">전송 (대기열처리)</button>

            <input type="submit" value="대기열없이 전송">
        </form>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

    <?php if(defined('WG_FORM_GATE_ID')) : // 유량제어 include ?>
    <link  href="https://cdn.devy.kr/1023/css/webgate.css?v=21.1.0a" rel="stylesheet" />
    <script src="https://cdn.devy.kr/1023/js/webgate.js?v=21.1.0a"></script>
    <?php endif; ?>

    <script>
    function form_submit() {

        console.log('form submit');
        $('form').submit();
    }

    $('#submit-button').click(function(){

        if($('[name="name"]').val() == "") {
            alert("이름을 입력하세요");
            return false;
        }
        <?php if(defined('WG_FORM_GATE_ID')) : // 유량제어 front-end 처리 ?>
            console.log('유량제어 시스템 ready!')
            WG_StartWebGate(<?php echo WG_FORM_GATE_ID; ?>, form_submit);
        <?php else : ?>
            form_submit();
        <?php endif; ?>
    });
    </script>
</body>
</html>
