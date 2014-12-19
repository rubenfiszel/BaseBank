# Database creation
 
# --- !Ups
drop table if exists  Address;
create table Address (
	AddressID	INTEGER not null,
	Street		VARCHAR(45),
	City		VARCHAR(45),
	State		VARCHAR(15),
	Country		VARCHAR(15),	
    primary key	(AddressID)
);

drop table if exists Clients;
create table Clients (
	SSN 		VARCHAR(30) not null, 
	LName 		VARCHAR(30),
	Fname		VARCHAR(30),
	AddressID 	INTEGER not null,
    primary key (SSN),
    foreign key (AddressID) references Address (AddressID)
);

drop table if exists  Entity;
create table Entity (
	EntityID 	INTEGER not null,
	Ename		VARCHAR(45),
	Field		VARCHAR(15),
    primary key (EntityID)
);

drop table if exists  Branch;
create table Branch (
	BranchID    INTEGER not null,
	AddressID	INTEGER,
    primary key (BranchID),
    foreign key	(AddressID) references Address (AddressID)
);

drop table if exists  Account;
create table Account (
	Iban         	VARCHAR(50) not null,
	SSN				VARCHAR(30),
	BranchID		INTEGER,
	Balance			DOUBLE,
	PasswordAcc		VARCHAR(70),
    primary key 	(Iban),
    foreign key 	(SSN) references Clients (SSN),
	foreign key		(BranchID) references Branch (BranchID)
);

drop table if exists  Salary;
create table Salary (
	SalaryID   	INTEGER not null,
	AccountID	VARCHAR(30),
	EntityID	INTEGER,
	Amount		DOUBLE,
	TimeSalary	TIMESTAMP,
    primary key (SalaryID),
    foreign key (AccountID) references Account (Iban),
    foreign key (EntityID) references Entity (EntityID)
);

drop table if exists  Transfer;
create table Transfer (
	TransferID  	INTEGER not null AUTO_INCREMENT,
	SenderAcc		VARCHAR(30),
	ReceiverAcc		VARCHAR(30),
	Amount			DOUBLE,
	TimeTransfer	TIMESTAMP,
    primary key 	(TransferID)
);

drop table if exists  Payment;
create table Payment (
	PaymentID 		INTEGER not null,
	AccountID 		VARCHAR(30),
	EntityID		INTEGER,
	Amount 			DOUBLE,
	TimePayment 	TIMESTAMP,
    primary key 	(PaymentID),
    foreign key 	(AccountID) references Account (Iban),
    foreign key 	(EntityID) references Entity (EntityID)
);






DROP PROCEDURE IF EXISTS GetSth;

CREATE PROCEDURE GetSth(IN nscore INT)
BEGIN
	SELECT TransferID, Amount+nscore FROM Transfer ORDER BY Amount LIMIT 0,100;;
END;


DROP PROCEDURE IF EXISTS Statement;
CREATE PROCEDURE Statement(IN accountin VARCHAR(15))
BEGIN
	IF EXISTS ( SELECT Iban FROM account WHERE Iban = accountin) THEN
		SELECT 	*
		FROM 	
				((SELECT A.SenderAcc as AccountID, A.TransferID, "Debit: Transfer" as "TransactionType", A.Amount as Amount, A.TimeTransfer as "TransactionTime"
					FROM	transfer A
					WHERE	A.SenderAcc = accountin AND Month(A.TimeTransfer) = Month(CURRENT_TIMESTAMP)) UNION ALL
				(SELECT A.ReceiverAcc as AccountID, A.TransferID, "Credit: Transfer" as "TransactionType", A.Amount as Amount, A.TimeTransfer as "TransactionTime"
					FROM	transfer A 
					WHERE	A.ReceiverAcc = accountin AND Month(A.TimeTransfer) = Month(CURRENT_TIMESTAMP)) UNION ALL
				(SELECT A.AccountID as AccountID, A.SalaryID, "Credit: Salary" as "TransactionType", A.Amount as Amount, A.TimeSalary as "TransactionTime"
					FROM	salary A 
					WHERE	A.AccountID = accountin AND Month(A.TimeSalary) = Month(CURRENT_TIMESTAMP)) UNION ALL
				(SELECT A.AccountID as AccountID, A.PaymentID, "Debit: Payment" as "TransactionType", A.Amount as Amount, A.TimePayment as "TransactionTime"
					FROM	payment A
					WHERE	A.AccountID = accountin AND Month(A.TimePayment) = Month(CURRENT_TIMESTAMP))) as Result
		ORDER BY Result.TransactionTime;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;

DROP PROCEDURE IF EXISTS ExpenditureInField;
CREATE PROCEDURE ExpenditureInField(IN accountin VARCHAR(50), IN fieldin VARCHAR(15))
BEGIN
	IF EXISTS ( SELECT Iban FROM account WHERE Iban = accountin) THEN
		SELECT A.AccountID as AccountID, SUM(A.Amount) as Expenditure
		FROM	payment A, entity E
		WHERE	A.AccountID = accountin AND A.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH) 
			AND A.EntityID = E.EntityID AND E.Field = fieldin;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS ExpenditureByFields;
CREATE PROCEDURE ExpenditureByFields(IN accountin VARCHAR(50))
BEGIN
	IF EXISTS ( SELECT Iban FROM account WHERE Iban = accountin) THEN
		SELECT 	Field, SUM(A.Amount) as Expenditure
		FROM	payment A, entity E
		WHERE	A.AccountID = accountin AND A.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH) 
			AND A.EntityID = E.EntityID
		GROUP BY E.Field;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;


DROP PROCEDURE IF EXISTS MonthlyExpenditure;
CREATE PROCEDURE MonthlyExpenditure(IN accountin VARCHAR(50))
BEGIN
	IF EXISTS ( SELECT Iban FROM account WHERE Iban = accountin) THEN
		SELECT 	A.AccountID as AccountID, monthname(A.TimePayment) as "Month", SUM(A.Amount) as Expenditure
		FROM	payment A
		WHERE	A.AccountID = accountin AND A.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 MONTH)
		GROUP BY month(A.TimePayment)
		ORDER BY SUM(A.Amount) DESC;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS PaymentsOften;
CREATE PROCEDURE PaymentsOften(IN accountin VARCHAR(50))
BEGIN
	IF EXISTS ( SELECT Iban FROM account WHERE Iban = accountin) THEN
		SELECT 	E.EntityID, E.Ename as "Entity", count(distinct P.PaymentID) as "Count"
		FROM	account A, clients C, entity E, payment P
		WHERE	A.Iban = accountin AND A.SSN = C.SSN AND P.AccountID = A.Iban
				AND P.EntityID = E.EntityID and P.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH)
		GROUP BY E.EntityID;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS StatementPrediction;
CREATE PROCEDURE StatementPrediction(IN accountin VARCHAR(50))
BEGIN
	IF EXISTS (SELECT Iban FROM account WHERE Iban = accountin) THEN
		IF EXISTS(SELECT SalaryID FROM salary S WHERE S.AccountID = accountin AND Month(S.TimeSalary) =  Month(CURRENT_TIMESTAMP)) THEN
			#  ALREADY HAD SALARY THIS MONTH
			SELECT 	A.Iban, (A.Balance - SUM(ExpenditureLeft.AmountLeft)) as "EndOfMonthBalance"
			FROM
					(SELECT (X.Amount - Y.Amount) as "AmountLeft"
					FROM (
						SELECT 	Field, CEIL((SUM(A.Amount)/3)) as Amount
						FROM	payment A, entity E
						WHERE	A.AccountID = accountin AND Month(A.TimePayment) >= Month(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH))
								AND Month(A.TimePayment) <> Month(CURRENT_TIMESTAMP) AND Year(A.TimePayment) >= Year(CURRENT_TIMESTAMP)
								AND A.EntityID = E.EntityID
						GROUP BY E.Field
                        UNION ALL
                        SELECT		Field, "0" as Amount
							FROM 	entity E
							WHERE	E.Field not in (SELECT Field
								FROM	payment A, entity E
								WHERE	A.AccountID = accountin AND Month(A.TimePayment) >= Month(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH))
										AND Month(A.TimePayment) <> Month(CURRENT_TIMESTAMP) AND Year(A.TimePayment) >= Year(CURRENT_TIMESTAMP)
										AND A.EntityID = E.EntityID
								GROUP BY E.Field)) AS X,              
						(SELECT 	Field, A.Amount as Amount
							FROM	payment A, entity E
							WHERE	A.AccountID = accountin AND Month(A.TimePayment) = Month(CURRENT_TIMESTAMP)
									AND A.EntityID = E.EntityID
							GROUP BY E.Field 
						UNION ALL
						SELECT		Field, "0" as Amount
							FROM 	entity E
							WHERE	E.Field not in (SELECT Field
								FROM	payment A, entity E
								WHERE	A.AccountID = accountin AND Month(A.TimePayment) = Month(CURRENT_TIMESTAMP)
										AND A.EntityID = E.EntityID
								GROUP BY E.Field )) AS Y
					WHERE X.Amount >= Y.Amount AND X.Field = Y.Field) as ExpenditureLeft,
					account A
			WHERE	A.Iban = accountin;;
		ELSE 
			#  NO SALARY THIS MONTH
			SELECT 	A.Iban, (A.Balance - SUM(ExpenditureLeft.AmountLeft) + S.Amount) as "EndOfMonthBalance"
			FROM
					(SELECT (X.Amount - Y.Amount) as "AmountLeft"
					FROM (
						SELECT Field, CEIL((SUM(A.Amount)/3)) as Amount
						FROM	payment A, entity E
						WHERE	A.AccountID = accountin AND Month(A.TimePayment) >= Month(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH))
								AND Month(A.TimePayment) <> Month(CURRENT_TIMESTAMP) AND Year(A.TimePayment) >= Year(CURRENT_TIMESTAMP)
								AND A.EntityID = E.EntityID
						GROUP BY E.Field
                        UNION ALL
                        SELECT	Field, "0" as Amount
							FROM 	entity E
							WHERE	E.Field not in (SELECT Field
								FROM	payment A, entity E
								WHERE	A.AccountID = accountin AND Month(A.TimePayment) >= Month(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH))
										AND Month(A.TimePayment) <> Month(CURRENT_TIMESTAMP) AND Year(A.TimePayment) >= Year(CURRENT_TIMESTAMP)
										AND A.EntityID = E.EntityID
								GROUP BY E.Field)) AS X,              
						(SELECT Field, A.Amount as Amount
							FROM	payment A, entity E
							WHERE	A.AccountID = accountin AND Month(A.TimePayment) = Month(CURRENT_TIMESTAMP)
									AND A.EntityID = E.EntityID
							GROUP BY E.Field 
						UNION ALL
						SELECT	Field, "0" as Amount
							FROM 	entity E
							WHERE	E.Field not in (SELECT Field
								FROM	payment A, entity E
								WHERE	A.AccountID = accountin AND Month(A.TimePayment) = Month(CURRENT_TIMESTAMP)
										AND A.EntityID = E.EntityID
								GROUP BY E.Field )) AS Y
					WHERE X.Amount >= Y.Amount AND X.Field = Y.Field) as ExpenditureLeft,
					account A, salary S
			WHERE	A.Iban = accountin AND S.AccountID = A.Iban;;
		END IF;;
	ELSE
		SELECT "Sorry, this Account was not found" AS "Error Message";;
	END IF;;
END;


DROP PROCEDURE IF EXISTS AccountsMostActivity;
CREATE PROCEDURE AccountsMostActivity()
BEGIN
	SELECT 	Result.AccountID as AccountID, SUM(Result.ActivityCount) as ActivityCount
	FROM 	
			((SELECT A.SenderAcc as AccountID, count(distinct A.TransferID) as ActivityCount
				FROM	transfer A, transfer B
				WHERE	A.SenderAcc = B.SenderAcc AND A.TimeTransfer > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH)
				GROUP BY A.SenderAcc) UNION ALL
			(SELECT A.ReceiverAcc as AccountID, count(distinct A.TransferID) as ActivityCount
				FROM	transfer A, transfer B 
				WHERE	A.ReceiverAcc = B.ReceiverAcc AND A.TimeTransfer > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH)
				GROUP BY A.ReceiverAcc) UNION ALL
			(SELECT A.AccountID as AccountID, count(distinct A.PaymentID) as ActivityCount
				FROM	payment A, payment B
				WHERE	A.AccountID = B.AccountID AND A.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH)
				GROUP BY A.AccountID)) as Result
	GROUP BY Result.AccountID
	ORDER BY SUM(Result.ActivityCount) DESC
	LIMIT 5;;
END;


DROP PROCEDURE IF EXISTS ClientsByBranch;
CREATE PROCEDURE AccountsByBranch(IN branchin INTEGER)
BEGIN
	IF EXISTS ( SELECT BranchID FROM branch WHERE BranchID=branchin) THEN
		SELECT 	B.BranchID, count(distinct C.SSN) as AccountsManaged
		FROM	clients C, account A, branch B
		WHERE	B.BranchID = A.BranchID AND A.SSN = C.SSN AND B.BranchID = branchin;;
	ELSE
		SELECT "Sorry, this Branch was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS AccountsByBranch;
CREATE PROCEDURE ClientsByBranch(IN branchin INTEGER)
BEGIN
	IF EXISTS ( SELECT BranchID FROM branch WHERE BranchID=branchin) THEN
		SELECT 	B.BranchID, count(distinct A.Iban) as ClientsManaged
		FROM	account A, branch B
		WHERE	B.BranchID = A.BranchID AND B.BranchID = branchin;;
	ELSE
		SELECT "Sorry, this Branch was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS MoneyByBranch;
CREATE PROCEDURE MoneyByBranch(IN branchin INTEGER)
BEGIN
	IF EXISTS ( SELECT BranchID FROM branch WHERE BranchID=branchin) THEN
		SELECT 	B.BranchID, SUM(A.Balance) as MoneyInBranch
		FROM	account A, branch B
		WHERE	B.BranchID = A.BranchID AND B.BranchID = branchin;;
	ELSE
		SELECT "Sorry, this Branch was not found" AS "Error Message";;
	END IF;;
END;



DROP PROCEDURE IF EXISTS MonthlyExpenditureByBranch;
CREATE PROCEDURE MonthlyExpenditureByBranch(IN branchin INTEGER)
BEGIN
	IF EXISTS ( SELECT BranchID FROM branch WHERE BranchID = branchin) THEN
		SELECT 	monthname(P.TimePayment) as "Month", SUM(P.Amount) as Expenditure
		FROM	payment P, account A
		WHERE	P.AccountID = A.Iban AND P.TimePayment > DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 MONTH)
			AND A.BranchID = branchin
		GROUP BY month(P.TimePayment)
		ORDER BY SUM(P.Amount) DESC;;
	ELSE
		SELECT "Sorry, this Branch was not found" AS "Error Message";;
	END IF;;
END;




DROP PROCEDURE IF EXISTS NegativeClients;
CREATE PROCEDURE NegativeClients()
BEGIN
	SELECT 	A.Iban, Fname as "First Name", LName as "Last Name", Balance
	FROM	account A, clients C
	WHERE	A.SSN = C.SSN AND A.Balance < 0;;
END;



DROP PROCEDURE IF EXISTS BranchMoney;
CREATE PROCEDURE BranchMoney()
BEGIN
	SELECT 	B.BranchID as "Branch", SUM(A.Balance) as "Money Managed"
	FROM	branch B, account A
	WHERE	A.BranchID = B.BranchID
	GROUP BY B.BranchID
	ORDER BY SUM(A.Balance) DESC;;
END;



DROP PROCEDURE IF EXISTS NegativeClientsByBranch;
CREATE PROCEDURE NegativeClientsByBranch()
BEGIN
	SELECT 	B.BranchID as "Branch", count(distinct C.SSN) as "Negative Clients"
	FROM	account A, clients C, branch B
	WHERE	A.SSN = C.SSN AND A.Balance < 0 AND A.BranchID = B.BranchID
	GROUP BY B.BranchID;;
END;



DROP PROCEDURE IF EXISTS WorkExpenditure;
CREATE PROCEDURE WorkExpenditure()
BEGIN
	SELECT 	A.SSN, ((A.Expenditure) * (1/B.Expenditure) * 100) AS "Expenditure in Field (%)"
	FROM 	
			(SELECT 	C.SSN, SUM(P.Amount) as "Expenditure"
				FROM	clients C, account A, payment P, entity EP, entity ES, salary S
				WHERE	C.SSN = A.SSN AND A.Iban = P.AccountID AND P.EntityID = EP.EntityID
						AND S.AccountID = A.Iban AND S.EntityID = ES.EntityID AND ES.Field = EP.Field
				GROUP BY C.SSN) as A,
			(SELECT 	C.SSN, SUM(Amount) as "Expenditure"
				FROM	clients C, account A, payment P
				WHERE	C.SSN = A.SSN AND A.Iban = P.AccountID
				GROUP BY C.SSN) as B
	WHERE A.SSN = B.SSN;;
END;




# --- !Downs

