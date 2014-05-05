function uploadCallback(response) {
    if (response.success) {
        if (response.data.init_serverconf) {
            $("#globalconf_form").hide();
            $("#serverconf_form").show();

            populateOwnerSelect();
        } else {
            alert("Security Server configured. Redirecting..", null, function() {
                redirect("clients");
            });
        }
    }

    showMessages(response.messages);
}

function populateOwnerSelect() {
    $.get(action("member_classes"), null, function(response) {
        var select = $("#owner_class").html("");

        $.each(response.data, function() {
            select.append($("<option />").val(this).text(this));
        });
    });

    $.get(action("member_codes"), null, function(response) {
        $("#owner_code").autocomplete("option", "source", response.data);
    });
}

$(document).ready(function() {
    if ($("#globalconf_form").length > 0) {
        $("#serverconf_form").hide();
    }

    var namefetch = function() {
        var timer = 0;
        return function(callback, ms) {
            clearTimeout(timer);
            timer = setTimeout(callback, ms);
        };
    }();

    $("#owner_class").change(function() {
        $("#owner_code").keyup();
    });

    $("#owner_code").autocomplete({
        delay: 0,
        minLength: 0,
        select: function() {
            $("#owner_code").keyup();
        },
    });

    $("#owner_code").keyup(function() {
        namefetch(function () {
            var params = $("#owner_class, #owner_code").serialize();
            $.get(action("member_name"), params, function(response) {
                $("#owner_name").val(response.data.name);
            });
        }, 500);        
    });

    populateOwnerSelect();

    $("#submit_serverconf").click(function() {
        var params = $("#serverconf_form").serialize();
        $.post(action("init_serverconf"), params, function() {
            alert("Security Server configured. Redirecting..", null, function() {
                redirect("clients");
            });
        });

        return false;
    });
});
