create index if not exists idx_users_email on users(email);
create index if not exists idx_users_otp on users(otp);

create index if not exists idx_trx_user_month_direction on transactions(user_id, month, direction);

create index if not exists idx_income_user_month_source on income(user_id, month, source);

create index if not exists idx_expense_user_month_category on expense(user_id, month, category);

create index if not exists idx_budget_user_month_category on budget(user_id, month, category);

create index if not exists idx_bs_user on bank_statement(user_id);
create index if not exists idx_bs_user_month on bank_statement(user_id, month);