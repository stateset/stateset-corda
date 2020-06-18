# Stateset

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Stateset is a distributed sales and finance automation network. In order to use the Stateset Platform interface go to https://stateset.io/signup

The Stateset Network Swagger API can be found at https://stateset.network:8080/swagger-ui.html#/

```
    _____       _____                 _____
    _________  /______ __  /___________________  /_
    __  ___/  __/  __ `/  __/  _ \_  ___/  _ \  __/
    _(__  )/ /_ / /_/ // /_ /  __/(__  )/  __/ /_
    /____/ \__/ \__,_/ \__/ \___//____/ \___/\__/


	   8 Node Network Graph | 28 Edges | 1 Notary
-------------------------------------------------------------------
          ^      ^     ^      ^     ^      ^
	 /--------\   /--------\   /--------\                                   
	|	   | |	        | |          |                  
	|  PartyB  | |  PartyC  | |  PartyD  | 
	|          | |	   	| |          |                   
 	 \--------/   \--------/   \--------/
  ^      ^       v     ^      ^     v       ^      ^ 
 /--------\	      /--------\	   /--------\
|	   |	     |	        |	  |	     |
|  PartyA  |	     | Notaries |	  |  PartyE  | 
|	   |	     |	        |	  |	     | 
 \--------/	      \--------/           \--------/
  v       ^      ^     ^      ^     ^      ^       v
	 /--------\   /--------\   /--------\                                   
	|	   | |	        | |          |                            
	|  PartyH  | |  PartyG  | |  PartyF  | 
	|          | |	        | |          |                             
 	 \--------/   \--------/   \--------/
          v      v     v      v     v      v
--------------------------------------------------------------------

                ^  +-------------------------------+  ^
                |  |                               |  |
                |  |  State-machine = Application  |  |
                |  |                               |  |   Stateset
                |  |            ^      +           |  |
                |  +----------- | ABCI | ----------+  v
                |  |            +      v           |  ^
                |  |                               |  |
Stateset        |  |           Consensus           |  |
Notaries        |  |                               |  |
Clusters        |  +-------------------------------+  |   Tendermint Core
                |  |                               |  |
                |  |           Networking          |  |
                |  |                               |  |
                v  +-------------------------------+  v


```


### Stateset Network Setup


1) Install the Stateset Network locally via Git:

```bash

git clone https://github.com/stateset/stateset

```

2) Deploy the Nodes


```bash

cd stateset && gradlew.bat deployNodes (Windows) OR ./gradlew deployNodes (Linux)

```

3) Run the Nodes

```bash

cd workflows
cd build 
cd nodes
runnodes.bat (Windows) OR ./runnodes (Linux)

```
4) Run the Spring Boot Server

```bash

cd ..
cd ..
cd server
../gradlew.bat bootRun -x test (Windows) OR ../gradlew bootRun -x test

```
The Stateset Network API Swagger will be running at `http://localhost:8080/swagger-ui.html#/`

To change the name of your `organisation` or any other parameters, edit the `node.conf` file and repeat the above steps.

### Joining the Network

Add the following to the `node.conf` file:

`compatibilityZoneUrl="https://stateset.network:8080"`

This is the current network map and doorman server URL

1) Remove Existing Network Parameters and Certificates

```bash

cd build
cd nodes
cd PartyA
rm -rf persistence.mv.db nodeInfo-* network-parameters certificates additional-node-infos

```

2) Download the Network Truststore

```bash

curl -o /var/tmp/network-truststore.jks https://stateset.network:8080//network-map/truststore

```

3) Initial Node Registration

```bash

java -jar corda.jar --initial-registration --network-root-truststore /var/tmp/network-truststore.jks --network-root-truststore-password trustpass

```
4) Start the Node

```bash

java -jar corda.jar

```

To teardown all of the nodes and server:

```bash

taskkill /f /im java.exe

```

#### Node Configuration

Configuration 

- Corda version: Corda 4
- Vault SQL Database: PostgreSQL
- Cloud Service Provider: GCP
- JVM or Kubernetes


#### Database Configuration

The node configuration on deploy is setup to leverage Postgres using the Cloud SQL Instance

```bash

   node {
        name "O=StateSet,L=San Mateo,C=US"
        p2pPort 10011
        rpcSettings {
            address("localhost:10030")
            adminAddress("localhost:10070")
        }
        cordapps = []
        rpcUsers = [[ user: "user1", "password": "test", "permissions": ["ALL"]]]
        extraConfig = [
                'dataSourceProperties' : [
                        'dataSourceClassName' : 'org.postgresql.ds.PGSimpleDataSource',
                        'dataSource.url' : 'jdbc:postgresql://<DATABASE_NAME>?cloudSqlInstance=<INSTANCE>&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=<USER>&password=<PASSWORD>&currentSchema=stateset',
                        'dataSource.password' : 'password',
                        'dataSource.user' : 'user'
                ],
                'database': [
                        'transactionIsolationLevel' : 'READ_COMMITTED'
                ],
                'jarDirs': '[/stateset-network/drivers]'
        ]
        webPort 8080
        // extraConfig = ['h2Settings.address' : 'localhost:12347']
        webserverJar = "${rootProject.projectDir}/server/build/libs/server-${project.version}.jar"
    }
```

In order to use the GraphQL engine on the Stateset Network; the schema, user and password will need to be created on Postgres:

```bash

CREATE USER "my_user" WITH LOGIN PASSWORD 'my_password';
CREATE SCHEMA "my_schema";
GRANT USAGE, CREATE ON SCHEMA "my_schema" TO "my_user";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "my_schema" TO "my_user";
ALTER DEFAULT privileges IN SCHEMA "my_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "my_user";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "my_schema" TO "my_user";
ALTER DEFAULT privileges IN SCHEMA "my_schema" GRANT USAGE, SELECT ON sequences TO "my_user";
ALTER ROLE "my_user" SET search_path = "my_schema";

```

### Network States

Customer States are transferred between stakeholders on the network.

#### Accounts

The first state to be deployed on the network is the `Account`. Version 0.1 of the `Account` State has the following structure:

```jsx

// *********
// * Account State *
// *********

data class Account(val accountId: String,
                   val accountName: String,
                   val accountType: TypeOfBusiness,
                   val industry: String,
                   val phone: String,
                   val yearStarted: Int,
                   val annualRevenue: Double,
                   val businessAddress: String,
                   val businessCity: String,
                   val businessState: String,
                   val businessZipCode: String,
                   val controller: Party,
                   val processor: Party ) : ContractState, QueryableState {


```

The Account has the following business `flows` that can be called:

- `CreateAccount` - Create an Account between your organization and a known counterparty on the DSOA
- `TransferAccount` - Transfer the Account between your organization and a counterparty on the DSOA
- `ShareAccount` - Share the Account Data with a counterparty
- `EraseAccount` - Erase the Account Data

#### Contacts

The second state to be deployed on the network is the `Contact`. Version 0.1 of the `Contact` State has the following structure:

```jsx

// *********
// * Contact State *
// *********

data class Contact(val contactId: String,
                   val firstName: String,
                   val lastName: String,
                   val email: String,
                   val phone: String,
                   val controller: Party,
                   val processor: Party,
                   override val linearId: UniqueIdentifier = UniqueIdentifier())


```


The Contact has the following business `flows` that can be called:

- `CreateContact` - Create a Contact between your organization and a known counterparty on the DSOA
- `TransferContact` - Transfer the Contact between your organization and a counterparty on the DSOA
- `ShareContact` - Share the Contact Data with a counterparty
- `EraseContact` - Erase the Contact Data

#### Leads

The third state to be deployed on the network is the `Lead`. Version 0.1 of the `Lead` State has the following structure:

```jsx

// *********
// * Lead State *
// *********

data class Lead(val leadId: String,
                val firstName: String,
                val lastName: String,
                val company: String,
                val title: String,
                val email: String,
                val phone: String,
                val country: String,
                val controller: Party,
                val processor: Party,
                override val linearId: UniqueIdentifier = UniqueIdentifier())


```


The Lead has the following business `flows` that can be called:

- `CreateLead` - Create a Lead between your organization and a known counterparty on the DSOA
- `TransferLead` - Transfer the Lead between your organization and a counterparty on the DSOA
- `ShareLead` - Share the Lead Data with a counterparty
- `EraseLead` - Erase the Lead Data
- `ConvertLead` - Convert a Lead State into an Account State and Contact State


We created the `Carmen Dashboard` to provide the ability for organizations to create `Accounts`, `Contacts`, and `Leads` with counterparties on the network.


#### Cases


```jsx

// *********
// * Case State *
// *********

data class Case(val caseId: String,
                val description: String,
                val caseNumber: String,
                val caseStatus: CaseStatus,
                val casePriority: CasePriority,
                val submitter: Party,
                val resolver: Party,
                override val linearId: UniqueIdentifier = UniqueIdentifier()) 


```

The Case has the following business `flows` that can be called:

- `CreateCase` - Create a Case between your organization and a known counterparty on the DSOA
- `StartCase` - Start on an unstarted Case
- `CloseCase` - Close the Case with a counterparty
- `EscalateCase` - Escalate the Case


#### Proposals

// *****************
// * Proposal State *
// *****************

```jsx

@BelongsToContract(ProposalContract::class)
data class Proposal(val proposalNumber: String,
                    val proposalName: String,
                    val proposalHash: String,
                    val proposalStatus: ProposalStatus,
                    val proposalType: ProposalType,
                    val totalProposalValue: Int,
                    val party: Party,
                    val counterparty: Party,
                    val proposalStartDate: String,
                    val proposalEndDate: String,
                    val active: Boolean?,
                    val createdAt: String?,
                    val lastUpdated: String?,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {

```

The Proposal has the following business `flows` that can be called:

- `CreateProposal` - Create a Proposal between your organization and a known counterparty on Stateset
- `AcceptProposal` - Accepts the Proposal
- `RejectProposal` - Rejects the Proposal



#### Agreements


```jsx

// *****************
// * Agreement State *
// *****************

@BelongsToContract(AgreementContract::class)
data class Agreement(val agreementNumber: String,
                     val agreementName: String,
                     val agreementHash: String,
                     val agreementStatus: AgreementStatus,
                     val agreementType: AgreementType,
                     val totalAgreementValue: Int,
                     val party: Party,
                     val counterparty: Party,
                     val agreementStartDate: String,
                     val agreementEndDate: String,
                     val active: Boolean?,
                     val createdAt: String?,
                     val lastUpdated: String?,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {


```

The Agreement has the following business `flows` that can be called:

- `CreateAgreement` - Create an Agreement between your organization and a known counterparty on Stateset
- `ActivateAgreement` - Activate the Agreement between your organization and a counterparty on Stateset
- `TerminateAgreement` - Terminate an existing or active agreement
- `RenewAgreement` - Renew an existing agreement that is or is about to expire
- `ExpireAgreement` - Expire a currently active agreement between you and a counterparty

The `Agreement Status` and `Agreement Type` enums are listed as follows:

```jsx


@CordaSerializable
enum class AgreementStatus {
    REQUEST, APPROVAL_REQUIRED, APPROVED, IN_REVIEW, ACTIVATED, INEFFECT, REJECTED, RENEWED, TERMINATED, AMENDED, SUPERSEDED, EXPIRED
}

@CordaSerializable
enum class AgreementType {
    NDA, MSA, SLA, SOW
}

```

#### Loans

```jsx


// *****************
// * Loan State *
// *****************

@BelongsToContract(LoanContract::class)
data class Loan(val loanNumber: String,
                val loanName: String,
                val loanReason: String,
                val loanStatus: LoanStatus,
                val loanType: LoanType,
                val amountDue: Int,
                val amountPaid: Int,
                val amountRemaining: Int,
                val subtotal: Int,
                val total: Int,
                val party: Party,
                val counterparty: Party,
                val dueDate: String,
                val periodStartDate: String,
                val periodEndDate: String,
                val paid: Boolean?,
                val active: Boolean?,
                val createdAt: String?,
                val lastUpdated: String?,
                override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {


```

The Loan has the following business `flows` that can be called:

- `CreateLoan` - Create a Loan between your organization and a known counterparty on Stateset
- `PayLoan` - Pay off a Loan


#### Invoices

```jsx


// *****************
// * Invoice State *
// *****************

@BelongsToContract(InvoiceContract::class)
data class Invoice(val invoiceNumber: String,
                   val invoiceName: String,
                   val billingReason: String,
                   val amountDue: Int,
                   val amountPaid: Int,
                   val amountRemaining: Int,
                   val subtotal: Int,
                   val total: Int,
                   val party: Party,
                   val counterparty: Party,
                   val dueDate: String,
                   val periodStartDate: String,
                   val periodEndDate: String,
                   val paid: Boolean?,
                   val active: Boolean?,
                   val createdAt: String?,
                   val lastUpdated: String?,
                   override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {


```

The Invoice has the following business `flows` that can be called:

- `CreateInvoice` - Create a Invoice between your organization and a known counterparty
- `PayInvoice` - Pay an Invoice
- `FactorInvoice` - Factor an Invoice


#### Purchase Orders

```jsx

// ************************
// * Purchase Order State *
// ************************

@BelongsToContract(PurchaseOrderContract::class)
data class PurchaseOrder(val purchaseOrderNumber: String,
                         val purchaseOrderName: String,
                         val purchaseOrderHash: String,
                         val purchaseOrderStatus: PurchaseOrderStatus,
                         val description: String,
                         val purchaseDate: String,
                         val deliveryDate: String,
                         val subtotal: Int,
                         val total: Int,
                         val purchaser: Party,
                         val vendor: Party,
                         val createdAt: String?,
                         val lastUpdated: String?,
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {

```

The Purchase Order has the following business `flows` that can be called:

- `CreatePurchaseOrder` - Create a PO between a purchaser and a vendor on Stateset
- `CompletePurchaseOrder` - Completes the PO
- `CancelPurchaseOrder` - Cancels the PO


Testing
-------

Testing and code review needs to be implemented on the Stateset Network.

### Automated Testing

Developers are strongly encouraged to write for new code, and to
submit new unit tests for old code. Unit tests can be compiled and run.

### Manual Quality Assurance (QA) Testing

Changes should be tested by somebody other than the developer who wrote the
code. This is especially important for large or high-risk changes. It is useful
to add a test plan to the pull request description if testing the changes is
not straightforward.
