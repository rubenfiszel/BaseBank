drop table if exists Client;
create table Client (
     SSN VARCHAR(30), 
     LName VARCHAR(30),
     Fname VARCHAR(30),
     AddressID INTEGER
);

drop table if exists  Entity;
create table Entity (
       EntityID 	INTEGER,
       Ename		VARCHAR(45),
       Field		VARCHAR(15)
);
drop table if exists  Salary;
create table Salary (
       SalaryID   	INTEGER,
       AccountID	VARCHAR(30),
       EntityID		INTEGER,
       Amount		DOUBLE,
       TimeSalary	TIMESTAMP
);

drop table if exists  Transfer;

create table Transfer (
       TransferID  	VARCHAR(30),
       SenderAcc	VARCHAR(30),
       ReceiverAcc	VARCHAR(30),
       Amount		DOUBLE,
       TimeTransfer	TIMESTAMP
);

drop table if exists  Payment;

create table Payment (
       PaymentID INTEGER,
       AccountID VARCHAR(30),
       EntityID	INTEGER,
       Amount DOUBLE,
       TimePayment TIMESTAMP
);
      
drop table if exists  Account;

create table Account (
       Iban         VARCHAR(50),
       ClientID		VARCHAR(50),
       BranchID		INTEGER,
       Balance		DOUBLE,
       PasswordAcc	VARCHAR(70)
);

drop table if exists  Branch;

create table Branch (
       BranchID    	INTEGER,
       AddressID	INTEGER
);

drop table if exists  Address;

create table Address (
		AddressID	INTEGER,
		Street		VARCHAR(45),
        City		VARCHAR(45),
        State		VARCHAR(15),
        Country		VARCHAR(15)
);

drop table if exists  Statement;

create table Statement (
       StatementID 		INTEGER,
       AccountID		VARCHAR(30),
       MonthStatement   TIMESTAMP,
       Balance			DOUBLE
);
