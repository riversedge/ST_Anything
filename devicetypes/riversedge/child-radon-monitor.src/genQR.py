import qrcode

mnid = 'fkJQ' # "FFFF" is an example. you should replace it with yours
onboardingId = '001' # "111" is an example. you should replace it with yours
serialNumber = 'STDKtest0001' # "STDKtest0001" is an example. you should replace it with yours
qrUrl = 'https://qr.samsungiots.com/?m=' + mnid + '&s=' + onboardingId + '&r=' + serialNumber
qrcode.QRCode(box_size=10, border=4)
img = qrcode.make(qrUrl)
img.save(serialNumber + '.png')
