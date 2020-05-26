(func $printb (param $b i32)
	local.get $b
	(if
		(then
			i32.const 0
			i32.const 4
			call $prints
		)
		(else
			i32.const 4
			i32.const 5
			call $prints
		)
	)
)

(func $printi (param $i i32)
	(local $rev i32)
	(local $count i32)

	i32.const 0
	local.set $rev

	local.get $i
	i32.const 0
	i32.lt_s

	(if
		(then
			i32.const 45
			call $printc
			i32.const -1
			local.get $i
			i32.mul
			local.set $i
		)
	)

	(block $b1
		(loop $l1
			local.get $i
			i32.const 10
			i32.lt_u
			br_if $b1

			local.get $i
			i32.const 10
			i32.rem_u

			local.get $rev
			i32.const 10
			i32.mul
			i32.add
			local.set $rev

			local.get $i
			i32.const 10
			i32.div_u
			local.set $i

			local.get $count
			i32.const 1
			i32.add
			local.set $count

			br $l1
		)
	)

	local.get $i
	i32.const 10
	i32.rem_u

	i32.const 48
	i32.add
	call $printc

	(block $b2
		(loop $l2
			local.get $count
			i32.const 0
			i32.eq
			br_if $b2

			local.get $rev
			i32.const 10
			i32.rem_u

			i32.const 48
			i32.add
			call $printc

			local.get $rev
			i32.const 10
			i32.div_u
			local.set $rev

			local.get $count
			i32.const 1
			i32.sub
			local.set $count

			br $l2
		)
	)
)

(func $prints (param $off i32) (param $len i32)
	(loop $l1

		local.get $off
		i32.load
		call $printc

		local.get $off
		i32.const 1
		i32.add
		local.set $off

		local.get $len
		i32.const 1
		i32.sub
		local.set $len

		local.get $len
		i32.const 0
		i32.gt_s
		br_if $l1
	)
)

(data 0 (i32.const 0) "true")
(data 0 (i32.const 4) "false")
