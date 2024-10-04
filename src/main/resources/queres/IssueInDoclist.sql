SELECT issue.id, doc_number, issue_name, issue_type, project, department, contract, issue.status, revision, period, issue_comment, author_comment, (select stage_date as contract_due_date from issue_stages where issue_stages.issue_type = issue.issue_type and issue_stages.stage_name = period and issue_stages.id_project = ip.id)
 FROM issue
 LEFT JOIN issue_types ON issue.issue_type = issue_types.type_name
 RIGHT JOIN issue_projects as ip ON project = ip.name
 WHERE issue_types."visibility-documents" = 1 AND removed = 0 AND project = $project