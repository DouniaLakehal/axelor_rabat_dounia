<%@ page import="com.axelor.rpc.ActionRequest" %>
<%@ page import="com.axelor.rpc.ActionResponse" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
      integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
      integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
<script src="../../lib/jquery.ui/js/jquery.js"></script>
<link href="./static/css/main.css" rel="stylesheet">
<div class="container">
    <div class="row">
        <div class="col-12">
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        </div>
    </div>


    <input type="text" value="${h}" >

    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <th>title1</th>
            <th>title2</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${h}" var="x">
            <tr>
                <td>${x.key}</td>
                <td>${x.value}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>


</div>
<script type="text/javascript" src="./static/js/main.js"></script>
<script>
    $("document").ready(function(){
        var tbody = $(".table>tbody");
        console.log(tbody);
    });



    $.ajax({
        url: '/ws/dms/attachments/com.axelor.sale.db.Order',
        type: 'GET',
        Accept:'application/json'
    })
        .done(function(data) {
            console.log(data);
            console.log("success");
        })
        .fail(function() {
            console.log("error");
        })
        .always(function() {
            console.log("complete");
        });

</script>



