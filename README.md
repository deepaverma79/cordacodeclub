![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Property Investment CorDapp

Welcome to the Property Investment CorDapp. 
1) Create Fund http://localhost:10009/api/property/create-fund?fundStateValue=100&partyNames=PartyA&partyNames=PartyB (PUT)
2) Register Property http://localhost:10009/api/property/register-property?address=XYZ&propertyManager=PartyC (PUT)
3) Sell My Share of fund http://localhost:10009/api/property/sell-fund-share?fundId=<UUID_CREATED_FUND>&currentInvestor=PartyA&newInvestor=PartyC (PUT)
4) Pay Dividend http://localhost:10009/api/property/pay-dividend?amount=10&fundId=<UUID_CREATED_FUND>
5) View Registered Properties http://localhost:10009/api/property/my-properties (GET)
6) View My Dividend  http://localhost:10012/api/property/my-dividend (GET)
7) View Available funds http://localhost:10009/api/property/my-funds  (GET)
