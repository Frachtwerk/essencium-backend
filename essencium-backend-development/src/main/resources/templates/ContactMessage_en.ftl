<#import "Header.ftl" as header>
<#import "Footer.ftl" as footer>

<!doctype html>
<html>
<head>
    <meta name="viewport" content="width=device-width">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>${contactRequest.subject}</title>
    <@header.head />
</head>
<body class="" style="background-color: #f6f6f6; font-family: sans-serif; -webkit-font-smoothing: antialiased; font-size: 14px; line-height: 1.4; margin: 0; padding: 0; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;">
<span class="preheader" style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">${contactRequest.subject}</span>
<table border="0" cellpadding="0" cellspacing="0" class="body" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background-color: #f6f6f6;">
    <tr>
        <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">&nbsp;</td>
        <td class="container" style="font-family: sans-serif; font-size: 14px; vertical-align: top; display: block; Margin: 0 auto; max-width: 580px; padding: 10px; width: 580px;">
            <div class="content" style="box-sizing: border-box; display: block; Margin: 0 auto; max-width: 580px; padding: 10px;">

                <!-- START CENTERED WHITE CONTAINER -->
                <table class="main" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background: #ffffff; border-radius: 3px;">

                    <!-- START MAIN CONTENT AREA -->
                    <tr>
                        <td class="wrapper" style="font-family: sans-serif; font-size: 14px; vertical-align: top; box-sizing: border-box; padding: 20px;">
                            <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%;">
                                <tr>
                                    <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
                                        <p style="text-align:center;">
                                            <a href="${mailBranding.url}" style="text-decoration: none;color:black;">
                                                <img src="${mailBranding.logo}" style="max-width: 100px;">
                                                <br /><br />
                                                <span style="font-size: 20px;">${mailBranding.name}</span>
                                            </a>
                                        </p>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">Following message was sent from ${mailBranding.name}:</p>
                                        <table border="0" cellpadding="0" cellspacing="0" class="btn btn-primary" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; box-sizing: border-box;">
                                            <tbody>
                                            <tr>
                                                <td align="left" style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                                                    <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%;">
                                                        <tr>
                                                            <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
                                                                <p style="font-family: sans-serif; font-size: 14px; font-weight: bold; margin: 0; margin-bottom: 15px;">New message from ${contactRequest.name}:</p>
                                                                <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;"><b>Email-adress:</b> ${contactRequest.mailAddress}</p>
                                                                <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;"><b>Subject:</b> ${contactRequest.subject}</p>
                                                                <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;"><b>Message:</b></p>
                                                                <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">${contactRequest.message}</p>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                            </tbody>
                                        </table>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">Best regards</p>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;"><i>Your team from ${mailBranding.name}</i></p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- END MAIN CONTENT AREA -->
                </table>

                <!-- START FOOTER -->
                <@footer.footerDiv/>
                <!-- END FOOTER -->

                <!-- END CENTERED WHITE CONTAINER -->
            </div>
        </td>
        <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">&nbsp;</td>
    </tr>
</table>
</body>
</html>