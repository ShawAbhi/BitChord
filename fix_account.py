import os

filepath = 'app/src/main/java/com/music/bitchord/ui/components/AccountAlerts.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

bad_block = '''            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            PillTextField(
                value = nameValue,
                onValueChange = onNameChange,
                placeholder = "Custom Tab Name (Optional)",
                enabled = !testing,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(8.dp))
            PillTextField(
                value = urlValue,
                onValueChange = onUrlChange,
                placeholder = urlPlaceholder,
                enabled = !testing,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(8.dp))
            PillTextField(
                value = usernameValue,
                onValueChange = onUsernameChange,
                placeholder = stringResource(R.string.username) + " (Optional)",
                enabled = !testing,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(8.dp))
            PillTextField(
                value = passwordValue,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.password) + " (Optional)",
                enabled = !testing,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit && !testing) onSave() }),
            )
        }'''

good_block = '''            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }'''

if bad_block in content:
    content = content.replace(bad_block, good_block)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Fixed AccountAlerts')
else:
    print('Not found')
